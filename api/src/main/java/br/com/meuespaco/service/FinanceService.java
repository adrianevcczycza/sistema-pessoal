package br.com.meuespaco.service;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.meuespaco.repository.RecordRepository;
import br.com.meuespaco.dto.RecordInput;
@Service
public class FinanceService {
 private final RecordRepository repo;private final RecordService records;
 public FinanceService(RecordRepository repo,RecordService records){this.repo=repo;this.records=records;}
 @Transactional public Object installments(String owner,RecordInput input){
  repo.lockOwner(owner);var source=input.data();records.validate("transactions",source);int count;try{count=RecordService.number(source,"installments").intValueExact();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Número de parcelas inválido.");}if(count<2||count>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe de 2 a 120 parcelas.");
  var total=RecordService.number(source,"amount");var unit=total.divide(BigDecimal.valueOf(count),2,RoundingMode.DOWN);var result=new ArrayList<Map<String,Object>>();var date=LocalDate.parse(source.get("date").toString());
  for(int i=0;i<count;i++){var id=UUID.nameUUIDFromBytes((input.id()+":installment:"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));var existing=repo.list(owner,"transactions").stream().filter(r->r.get("id").equals(id.toString())).findFirst();if(existing.isPresent()){result.add(existing.get());continue;}var d=new LinkedHashMap<>(source);d.put("title",source.get("title")+" ("+(i+1)+"/"+count+")");d.put("amount",i==count-1?total.subtract(unit.multiply(BigDecimal.valueOf(count-1))):unit);d.put("date",date.plusMonths(i).toString());d.put("dueDate",date.plusMonths(i).toString());d.put("paid",i==0&&Boolean.TRUE.equals(source.get("paid")));d.put("installmentGroup",input.id().toString());d.remove("installments");d.put("recurrence","Não repetir");result.add(records.save(owner,"transactions",new RecordInput(id,d,0)));}return result;
 }
 @Transactional public Object nextRecurring(String owner,UUID id){repo.lockOwner(owner);var source=RecordService.data(repo.find(owner,"transactions",id));var data=new LinkedHashMap<>(source);var next=LocalDate.parse(source.get("date").toString()).plusMonths(1);data.put("date",next.toString());data.put("paid",false);if(source.get("dueDate")!=null&&!source.get("dueDate").toString().isBlank())data.put("dueDate",LocalDate.parse(source.get("dueDate").toString()).plusMonths(1).toString());var nextId=UUID.nameUUIDFromBytes(("next-transaction:"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8));var existing=repo.list(owner,"transactions").stream().filter(r->r.get("id").equals(nextId.toString())).findFirst();return existing.isPresent()?existing.get():records.save(owner,"transactions",new RecordInput(nextId,data,0));}
 public Object annual(String owner,int year){if(year<1900||year>2200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);var months=new ArrayList<Map<String,Object>>();for(int m=1;m<=12;m++)months.add(records.summary(owner,YearMonth.of(year,m).toString()));return months;}
}
