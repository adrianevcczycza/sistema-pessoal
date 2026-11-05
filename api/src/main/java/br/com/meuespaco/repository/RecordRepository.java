package br.com.meuespaco.repository;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Repository
public class RecordRepository {
 private final JdbcTemplate db;private final ObjectMapper mapper;
 public RecordRepository(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
 public Map<String,Object> parse(String json){try{return mapper.readValue(json,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException("Registro inválido");}}
 public String json(Object data){try{return mapper.writeValueAsString(data);}catch(Exception e){throw new IllegalArgumentException("Conteúdo inválido");}}
 public List<Map<String,Object>> list(String owner,String module){return db.query("select * from records where owner=? and module=? order by created_at desc",(rs,n)->{var r=new LinkedHashMap<String,Object>();r.put("id",rs.getString("id"));r.put("data",parse(rs.getString("data")));r.put("version",rs.getLong("version"));r.put("createdAt",rs.getTimestamp("created_at").toInstant());r.put("updatedAt",rs.getTimestamp("updated_at").toInstant());return r;},owner,module);}
 public Map<String,Object> find(String owner,String module,UUID id){return list(owner,module).stream().filter(r->r.get("id").equals(id.toString())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registro não encontrado."));}
 public void lockOwner(String owner){db.queryForObject("select email from app_user where email=? for update",String.class,owner);}
 public Map<String,Object> save(String owner,String module,UUID id,Map<String,Object> data,long version){
  var existing=db.queryForList("select id from records where id=?",id);
  if(existing.isEmpty())db.update("insert into records(id,owner,module,data) values (?,?,?,cast(? as jsonb))",id,owner,module,json(data));
  else {int changed=db.update("update records set data=cast(? as jsonb),version=version+1,updated_at=now() where id=? and owner=? and module=? and version=?",json(data),id,owner,module,version);if(changed!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"O registro mudou. Atualize a página antes de salvar.");}
  db.update("insert into record_history(record_id,owner,data) values (?,?,cast(? as jsonb))",id,owner,json(data));return find(owner,module,id);
 }
 public void delete(String owner,String module,UUID id){if(db.update("delete from records where owner=? and module=? and id=?",owner,module,id)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND);db.update("delete from record_history where record_id=? and owner=?",id,owner);}
 public List<Map<String,Object>> history(String owner,UUID id){return db.query("select data,recorded_at from record_history where owner=? and record_id=? order by recorded_at desc",(rs,n)->Map.of("data",parse(rs.getString(1)),"recordedAt",rs.getTimestamp(2).toInstant()),owner,id);}
 public Map<String,Object> preferences(String owner){return db.query("select data from preferences where owner=?",(rs,n)->parse(rs.getString(1)),owner).stream().findFirst().orElse(Map.of("theme","rose","timezone","America/Sao_Paulo","waterGoal",2000));}
 public void preferences(String owner,Map<String,Object> data){db.update("insert into preferences(owner,data) values (?,cast(? as jsonb)) on conflict(owner) do update set data=excluded.data",owner,json(data));}
}
