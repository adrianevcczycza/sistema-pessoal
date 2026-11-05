package br.com.meuespaco.service;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import br.com.meuespaco.repository.RecordRepository;
@Service
public class HouseAnalytics {
 private final RecordService records;private final RecordRepository repo;
 public HouseAnalytics(RecordService records,RecordRepository repo){this.records=records;this.repo=repo;}
 @SuppressWarnings("unchecked") public Object calculate(String owner){
  var summary=records.house(owner);var items=(List<Map<String,Object>>)summary.get("items");var prefs=repo.preferences(owner);var rooms=new TreeMap<String,Map<String,Object>>();var priorities=new TreeMap<String,Map<String,Object>>();var periods=new TreeMap<String,Map<String,Object>>();
  for(var r:items){var d=RecordService.data(r);if(Boolean.TRUE.equals(d.get("notNeeded")))continue;for(var entry:List.of(Map.entry(rooms,d.getOrDefault("room","Sem ambiente").toString()),Map.entry(priorities,d.getOrDefault("priority","Sem prioridade").toString()),Map.entry(periods,d.getOrDefault("period","Sem período").toString()))){var group=entry.getKey().computeIfAbsent(entry.getValue(),key->{var v=new LinkedHashMap<String,Object>();v.put("name",key);v.put("active",0);v.put("resolved",0);v.put("pending",BigDecimal.ZERO);return v;});group.put("active",(Integer)group.get("active")+1);if(Boolean.TRUE.equals(d.get("resolved")))group.put("resolved",(Integer)group.get("resolved")+1);group.put("pending",((BigDecimal)group.get("pending")).add(RecordService.number(d,"missing").multiply(RecordService.number(d,"price"))));}}
  var result=new LinkedHashMap<String,Object>();result.put("rooms",rooms.values());result.put("priorities",priorities.values());result.put("periods",periods.values());result.put("plannedBudget",prefs.get("houseBudget"));result.put("availableBudget",prefs.get("houseBudget")==null||prefs.get("houseBudget").toString().isBlank()?null:RecordService.number(prefs,"houseBudget").subtract((BigDecimal)summary.get("spent")));return result;
 }
}
