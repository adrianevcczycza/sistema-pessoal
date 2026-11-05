package br.com.meuespaco.service;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.meuespaco.repository.RecordRepository;
import br.com.meuespaco.dto.RecordInput;
@Service
public class ImportService {
 private final RecordRepository repo;private final RecordService service;
 public ImportService(RecordRepository repo,RecordService service){this.repo=repo;this.service=service;}
 private static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).trim();}
 private static String field(String s){s=normalize(s);if(s.contains("ambiente"))return "room";if(s.equals("item")||s.contains("item ou kit")||s.equals("item/kit"))return "title";if(s.contains("quantidade")||s.equals("qtd"))return "needed";if(s.contains("teto")||s.contains("unitario"))return "price";if(s.contains("prioridade"))return "priority";if(s.contains("quando"))return "period";if(s.contains("observa"))return "description";if(s.contains("situacao")||s.equals("status"))return "importedStatus";if(s.contains("magalu"))return "magalu";if(s.contains("mercado"))return "mercadoLivre";if(s.contains("amazon"))return "amazon";return "source:"+s;}
 public Object preview(String owner,MultipartFile file) throws Exception {
  if(file.getSize()>10*1024*1024 || !Objects.toString(file.getOriginalFilename(),"").toLowerCase().endsWith(".xlsx"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie a planilha no formato .xlsx, até 10 MB.");
  var output=new ArrayList<Map<String,Object>>();var formatter=new DataFormatter(Locale.forLanguageTag("pt-BR"));
  try(var book=WorkbookFactory.create(file.getInputStream())){
   Map<String,Map<String,Object>> links=new HashMap<>();
   for(var sheet:book){if(!normalize(sheet.getSheetName()).contains("links completos"))continue;Map<Integer,String> headers=new HashMap<>();for(var row:sheet){if(headers.isEmpty()){for(var cell:row)headers.put(cell.getColumnIndex(),field(formatter.formatCellValue(cell)));continue;}var d=new LinkedHashMap<String,Object>();for(var cell:row){String key=headers.get(cell.getColumnIndex());String value=formatter.formatCellValue(cell);if(key!=null && (key.equals("title")||key.equals("room")||value.matches("https?://[^\\s]+")))d.put(key,value);}if(d.containsKey("title"))links.put(normalize(d.get("title").toString()),d);}}
   for(var sheet:book){if(normalize(sheet.getSheetName()).contains("links"))continue;Map<Integer,String> headers=new HashMap<>();for(var row:sheet){
    if(headers.isEmpty()){var candidate=new HashMap<Integer,String>();for(var cell:row)candidate.put(cell.getColumnIndex(),field(formatter.formatCellValue(cell)));if(candidate.containsValue("title") && candidate.containsValue("needed"))headers=candidate;continue;}
    var d=new LinkedHashMap<String,Object>();for(var cell:row){String key=headers.get(cell.getColumnIndex());if(key==null || cell.getCellType()==CellType.ERROR)continue;String value=formatter.formatCellValue(cell).trim();if(value.isEmpty())continue;if(Set.of("needed","price").contains(key) && cell.getCellType()==CellType.NUMERIC)d.put(key,java.math.BigDecimal.valueOf(cell.getNumericCellValue()));else d.put(key,value);}
    if(!d.containsKey("title") || !d.containsKey("needed"))continue;
    d.putIfAbsent("room",sheet.getSheetName());d.put("owned",0);d.put("gifted",0);d.put("sourceSheet",sheet.getSheetName());d.put("sourceRow",row.getRowNum()+1);
    var link=links.get(normalize(d.get("title").toString()));if(link!=null)for(var key:List.of("magalu","mercadoLivre","amazon"))if(link.containsKey(key))d.put(key,link.get(key));
    for(var key:List.of("magalu","mercadoLivre","amazon"))if(d.containsKey(key) && !d.get(key).toString().matches("https?://[^\\s]+"))d.remove(key);
    var id=UUID.nameUUIDFromBytes(("house-import:"+normalize(d.get("room").toString())+":"+normalize(d.get("title").toString())).getBytes(StandardCharsets.UTF_8));
    var existing=repo.list(owner,"house").stream().filter(r->r.get("id").equals(id.toString())).findFirst();var r=new LinkedHashMap<String,Object>();r.put("id",id);r.put("data",d);r.put("version",existing.map(RecordService::version).orElse(0L));r.put("status",existing.isPresent()?"existing":"new");r.put("valid",true);try{service.validate("house",d);}catch(ResponseStatusException e){r.put("valid",false);r.put("error",e.getReason());}output.add(r);
    if(output.size()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A planilha excede 1.000 itens.");
   }}
  }return Map.of("items",output,"count",output.size(),"expectedCount",110,"message",output.size()==110?"110 itens encontrados. Confira antes de confirmar.":"A quantidade encontrada difere dos 110 itens esperados. Confira as abas e os cabeçalhos. Valores e situações originais ficam preservados; compras precisam ser conferidas.");
 }
 @Transactional public Object confirm(String owner,List<RecordInput> items){if(items.size()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);repo.lockOwner(owner);int count=0;for(var item:items){if(repo.list(owner,"house").stream().anyMatch(r->r.get("id").equals(item.id().toString())))throw new ResponseStatusException(HttpStatus.CONFLICT,"Um item já existe. Revise a importação; nada foi confirmado.");service.save(owner,"house",item);count++;}return Map.of("imported",count);}
}
