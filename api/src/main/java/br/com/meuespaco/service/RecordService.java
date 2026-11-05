package br.com.meuespaco.service;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.meuespaco.dto.RecordInput;
import br.com.meuespaco.repository.RecordRepository;

@Service
public class RecordService {
 public static final Set<String> MODULES=Set.of("tasks","transactions","accounts","budgets","investments","workouts","sessions","meals","recipes","groceries","water","notes","studySessions","projects","prompts","house","purchases","comparisons","documents");
 private final RecordRepository repo;
 public RecordService(RecordRepository repo){this.repo=repo;}
 public void module(String module){if(!MODULES.contains(module))throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
 public static BigDecimal number(Map<String,Object> data,String key){var v=data.get(key);if(v==null || v.toString().isBlank())return BigDecimal.ZERO;try{var n=new BigDecimal(v.toString());if(n.signum()<0 || n.precision()>18 || n.scale()>4)throw new IllegalArgumentException();return n;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valor inválido: "+key);}}
 public static void required(Map<String,Object> data,String key){if(data.get(key)==null || data.get(key).toString().isBlank() || data.get(key).toString().length()>500)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Preencha corretamente: "+key);}
 @SuppressWarnings("unchecked") public static Map<String,Object> data(Map<String,Object> record){return (Map<String,Object>)record.get("data");}
 public static long version(Map<String,Object> r){return ((Number)r.get("version")).longValue();}
 public void validate(String module,Map<String,Object> d){
  required(d,"title");if(repo.json(d).length()>200000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Conteúdo muito grande.");
  for(var key:List.of("amount","price","quantity","needed","owned","gifted","freight","hours","load","sets","reps","rest","portions","ml","target","balance","installments"))if(d.containsKey(key))number(d,key);
  for(var key:List.of("amount","price","freight","target","balance"))if(d.containsKey(key)&&number(d,key).scale()>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valores monetários devem ter no máximo duas casas decimais.");
  for(var key:List.of("done","paid","favorite","notNeeded","shopping","received","createExpense"))if(d.containsKey(key)&&!(d.get(key) instanceof Boolean))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Marcação inválida: "+key);
  if(d.containsKey("time")&&!d.get("time").toString().isBlank())try{LocalTime.parse(d.get("time").toString());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Horário inválido.");}
  if(d.containsKey("reminder")&&!d.get("reminder").toString().isBlank())try{LocalDateTime.parse(d.get("reminder").toString().replace(' ','T'));}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe o lembrete como AAAA-MM-DDTHH:mm.");}
  for(var key:List.of("date","dueDate","deadline","deliveryDate"))if(d.get(key)!=null && !d.get(key).toString().isBlank()){try{LocalDate.parse(d.get(key).toString());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Data inválida: "+key);}}
  for(var key:List.of("url","magalu","amazon","mercadoLivre"))if(d.get(key)!=null && !d.get(key).toString().isBlank() && !d.get(key).toString().matches("https?://[^\\s]+"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use um link http ou https.");
  if(module.equals("transactions")){required(d,"amount");required(d,"date");if(!Set.of("Receita","Despesa","Transferência","Aporte").contains(d.get("type")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de lançamento inválido.");}
  if(module.equals("house") && number(d,"needed").signum()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe a quantidade necessária.");
  if(module.equals("tasks")&&Set.of("Diária","Semanal","Mensal").contains(d.getOrDefault("recurrence","")))required(d,"date");
  if(module.equals("purchases")){required(d,"houseId");required(d,"amount");required(d,"date");if(number(d,"quantity").signum()==0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe a quantidade comprada.");}
 }
 public List<Map<String,Object>> list(String owner,String module){module(module);var records=repo.list(owner,module);if(module.equals("projects"))for(var r:records){var d=data(r);var lines=d.getOrDefault("checklist","").toString().lines().filter(l->!l.isBlank()).toList();long done=lines.stream().filter(l->l.trim().startsWith("[x]")).count();d.put("progress",lines.isEmpty()?0:Math.round(100.0*done/lines.size()));d.put("completedSteps",done);d.put("totalSteps",lines.size());}if(module.equals("comparisons"))for(var r:records){var d=data(r);d.put("total",number(d,"price").multiply(number(d,"quantity")).add(number(d,"freight")));}return records;}
 @Transactional public Map<String,Object> save(String owner,String module,RecordInput input){
  module(module);if(module.equals("documents"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use o envio de arquivos.");
  repo.lockOwner(owner);var d=new LinkedHashMap<>(input.data());validate(module,d);
  if(module.equals("notes")&&d.containsKey("html"))d.put("html",org.jsoup.Jsoup.clean(d.get("html").toString(),org.jsoup.safety.Safelist.relaxed().addTags("pre","code","input","label","span").addAttributes("input","type","checked","disabled").addAttributes("ul","data-type").addAttributes("li","data-type","data-checked").addAttributes("td","colspan","rowspan").addAttributes("th","colspan","rowspan").removeTags("img").addEnforcedAttribute("a","rel","noopener noreferrer")));
  if(module.equals("purchases")){
   UUID houseId;try{houseId=UUID.fromString(d.get("houseId").toString());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Item da casa inválido.");}
   var house=data(repo.find(owner,"house",houseId));
   var acquired=repo.list(owner,"purchases").stream().filter(r->!r.get("id").equals(input.id().toString())).map(RecordService::data).filter(p->houseId.toString().equals(p.get("houseId"))).map(p->number(p,"quantity")).reduce(BigDecimal.ZERO,BigDecimal::add);
   var missing=number(house,"needed").subtract(number(house,"owned")).subtract(number(house,"gifted")).subtract(acquired).max(BigDecimal.ZERO);
   if(number(d,"quantity").compareTo(missing)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A compra excede a quantidade faltante.");
   var expenseId=UUID.nameUUIDFromBytes(("expense:"+input.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
   var existing=repo.list(owner,"transactions").stream().filter(r->r.get("id").equals(expenseId.toString())).findFirst();
   if(Boolean.TRUE.equals(d.get("createExpense"))){var expense=new LinkedHashMap<String,Object>();expense.put("title",d.get("title"));expense.put("amount",number(d,"amount").add(number(d,"freight")));expense.put("date",d.get("date"));expense.put("type","Despesa");expense.put("paid",true);expense.put("category","Nossa primeira casa");expense.put("purchaseId",input.id().toString());repo.save(owner,"transactions",expenseId,expense,existing.map(RecordService::version).orElse(0L));}
   else if(existing.isPresent())repo.delete(owner,"transactions",expenseId);
  }
  if(module.equals("transactions")){
   var old=repo.list(owner,module).stream().filter(r->r.get("id").equals(input.id().toString())).findFirst();
   if(d.containsKey("purchaseId") || old.map(RecordService::data).map(x->x.containsKey("purchaseId")).orElse(false))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Edite a compra vinculada na área da casa.");
  }
  var old=repo.list(owner,module).stream().filter(r->r.get("id").equals(input.id().toString())).findFirst();
  if(module.equals("groceries")&&d.containsKey("recipeIngredientId")&&old.isEmpty()){
   var duplicate=repo.list(owner,module).stream().filter(r->d.get("recipeIngredientId").equals(data(r).get("recipeIngredientId"))&&!Boolean.TRUE.equals(data(r).get("done"))).findFirst();if(duplicate.isPresent())return duplicate.get();
  }
  var saved=repo.save(owner,module,input.id(),d,input.version());
  if(module.equals("tasks")&&Boolean.TRUE.equals(d.get("done"))&&!old.map(RecordService::data).map(x->Boolean.TRUE.equals(x.get("done"))).orElse(false)&&Set.of("Diária","Semanal","Mensal").contains(d.getOrDefault("recurrence",""))){
   var next=new LinkedHashMap<>(d);var dateValue=Objects.toString(d.getOrDefault("date",d.get("dueDate")),"");if(!dateValue.isBlank()){
    LocalDate from=LocalDate.parse(dateValue);LocalDate to=switch(d.get("recurrence").toString()){case "Diária"->from.plusDays(1);case "Semanal"->from.plusWeeks(1);default->from.plusMonths(1);};next.put("date",to.toString());if(d.get("dueDate")!=null&&!d.get("dueDate").toString().isBlank())next.put("dueDate",LocalDate.parse(d.get("dueDate").toString()).plusDays(java.time.temporal.ChronoUnit.DAYS.between(from,to)).toString());next.put("done",false);next.remove("completedAt");next.remove("reminder");next.put("previousOccurrence",input.id().toString());var nextId=UUID.nameUUIDFromBytes(("next-task:"+input.id()).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(repo.list(owner,module).stream().noneMatch(r->r.get("id").equals(nextId.toString())))repo.save(owner,module,nextId,next,0);
   }
  }
  return saved;
 }
 @Transactional public void delete(String owner,String module,UUID id){module(module);repo.lockOwner(owner);var record=repo.find(owner,module,id);
  if(module.equals("documents"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a exclusão de arquivo.");
  if(module.equals("transactions") && data(record).containsKey("purchaseId"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Desfaça a compra vinculada.");
  if(module.equals("house") && repo.list(owner,"purchases").stream().map(RecordService::data).anyMatch(p->id.toString().equals(p.get("houseId"))))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Desfaça as compras vinculadas antes de excluir o item.");
  if(module.equals("purchases")){var expenseId=UUID.nameUUIDFromBytes(("expense:"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(repo.list(owner,"transactions").stream().anyMatch(r->r.get("id").equals(expenseId.toString())))repo.delete(owner,"transactions",expenseId);}
  repo.delete(owner,module,id);
 }
 public Map<String,Object> summary(String owner,String month){
  try{YearMonth.parse(month);}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mês inválido.");}
  var tx=repo.list(owner,"transactions").stream().map(RecordService::data).filter(d->d.getOrDefault("date","").toString().startsWith(month)).toList();
  BigDecimal income=BigDecimal.ZERO,expenses=BigDecimal.ZERO,planned=BigDecimal.ZERO;
  for(var t:tx){var amount=number(t,"amount");if(Boolean.TRUE.equals(t.get("paid"))){if("Receita".equals(t.get("type")))income=income.add(amount);if("Despesa".equals(t.get("type")))expenses=expenses.add(amount);}else if("Despesa".equals(t.get("type")))planned=planned.add(amount);}
  return Map.of("income",income,"expenses",expenses,"balance",income.subtract(expenses),"planned",planned,"month",month);
 }
 public Map<String,Object> house(String owner){
  var purchases=repo.list(owner,"purchases");var items=new ArrayList<Map<String,Object>>();BigDecimal spent=BigDecimal.ZERO,pending=BigDecimal.ZERO;int resolved=0,active=0,unknown=0;
  for(var purchase:purchases){var p=data(purchase);spent=spent.add(number(p,"amount")).add(number(p,"freight"));}
  for(var record:repo.list(owner,"house")){var d=new LinkedHashMap<>(data(record));var bought=purchases.stream().map(RecordService::data).filter(p->record.get("id").equals(p.get("houseId"))).map(p->number(p,"quantity")).reduce(BigDecimal.ZERO,BigDecimal::add);var available=number(d,"owned").add(number(d,"gifted")).add(bought);var missing=number(d,"needed").subtract(available).max(BigDecimal.ZERO);boolean ignored=Boolean.TRUE.equals(d.get("notNeeded"));boolean done=missing.signum()==0;
   if(!ignored){active++;if(done)resolved++;if(d.get("price")==null || d.get("price").toString().isBlank()){if(!done)unknown++;}else pending=pending.add(missing.multiply(number(d,"price")));}
   d.put("bought",bought);d.put("available",available);d.put("missing",ignored?BigDecimal.ZERO:missing);d.put("resolved",done);d.put("purchaseStatus",done?"Comprado":bought.signum()>0?"Parcialmente comprado":d.getOrDefault("purchaseStatus","A comprar"));var r=new LinkedHashMap<>(record);r.put("data",d);items.add(r);
  }return Map.of("items",items,"spent",spent,"pending",pending,"forecast",spent.add(pending),"active",active,"resolved",resolved,"unknownPrices",unknown,"progress",active==0?0:Math.round(100.0*resolved/active));
 }
}
