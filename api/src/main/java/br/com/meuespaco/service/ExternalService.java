package br.com.meuespaco.service;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import org.w3c.dom.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import br.com.meuespaco.repository.RecordRepository;
/** Provider URLs are server configuration, never supplied by requests. */
@Service
public class ExternalService {
 private final JdbcTemplate db;private final RecordRepository repo;private final String exchange,news;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final Map<String,Instant> attempts=new HashMap<>();
 public ExternalService(JdbcTemplate db,RecordRepository repo,@Value("${app.exchange-url}") String exchange,@Value("${app.news-url}") String news){this.db=db;this.repo=repo;this.exchange=exchange;this.news=news;}
 private String text(Element item,String tag){var nodes=item.getElementsByTagName(tag);return nodes.getLength()>0?nodes.item(0).getTextContent():"";}
 public synchronized Map<String,Object> get(String kind){
  var cached=db.query("select data,updated_at from external_cache where kind=?",(rs,n)->Map.<String,Object>of("data",repo.parse(rs.getString(1)),"updatedAt",rs.getTimestamp(2).toInstant()),kind).stream().findFirst();
  String url=kind.equals("exchange")?exchange:news;boolean fresh=cached.map(c->((Instant)c.get("updatedAt")).isAfter(Instant.now().minusSeconds(1800))).orElse(false);
  if(!url.isBlank() && !fresh && attempts.getOrDefault(kind,Instant.EPOCH).isBefore(Instant.now().minusSeconds(60))){
   attempts.put(kind,Instant.now());try{
    var response=client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("Accept",kind.equals("exchange")?"application/json":"application/rss+xml, application/xml").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    if(response.statusCode()!=200 || response.body().length>2_000_000)throw new IllegalStateException();
    Map<String,Object> data;
    if(kind.equals("exchange")){var raw=repo.parse(new String(response.body(),java.nio.charset.StandardCharsets.UTF_8));var rows=new ArrayList<Map<String,Object>>();for(var code:List.of("USDBRL","EURBRL")){var p=(Map<?,?>)raw.get(code);var bid=new java.math.BigDecimal(p.get("bid").toString());if(bid.signum()<=0)throw new IllegalStateException();rows.add(Map.of("currency",code.substring(0,3),"value",bid,"publishedAt",Instant.ofEpochSecond(Long.parseLong(p.get("timestamp").toString())).toString(),"variation",p.get("pctChange")));}data=Map.of("items",rows,"source",URI.create(url).getHost());}
    else {var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");var doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(response.body()));var nodes=doc.getElementsByTagName("item");var items=new ArrayList<Map<String,Object>>();var seen=new HashSet<String>();for(int i=0;i<Math.min(nodes.getLength(),100);i++){var item=(Element)nodes.item(i);String link=text(item,"link");if(!link.matches("https?://[^\\s]+") || !seen.add(link))continue;String date=text(item,"pubDate");Instant published;try{published=ZonedDateTime.parse(date,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();}catch(Exception e){continue;}items.add(Map.of("title",text(item,"title"),"url",link,"publishedAt",published.toString(),"source",URI.create(url).getHost(),"category",text(item,"category")));}data=Map.of("items",items,"source",URI.create(url).getHost());}
    db.update("insert into external_cache(kind,data) values (?,cast(? as jsonb)) on conflict(kind) do update set data=excluded.data,updated_at=now()",kind,repo.json(data));return Map.of("status","available","data",data,"updatedAt",Instant.now());
   }catch(Exception ignored){/* Never log provider URLs or private configuration. */}
  }
  if(cached.isPresent()){var out=new LinkedHashMap<>(cached.get());out.put("status",fresh?"available":"stale");return out;}
  return Map.of("status",url.isBlank()?"not_configured":"unavailable","message",url.isBlank()?"Integração não configurada.":"Não foi possível atualizar os dados.");
 }
}
