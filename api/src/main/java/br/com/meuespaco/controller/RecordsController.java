package br.com.meuespaco.controller;
import java.security.Principal;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.meuespaco.dto.RecordInput;
import br.com.meuespaco.repository.RecordRepository;
import br.com.meuespaco.service.RecordService;
@RestController @RequestMapping("/api")
public class RecordsController {
 private final RecordService service;private final RecordRepository repo;private final br.com.meuespaco.service.HouseAnalytics houseAnalytics;
 public RecordsController(RecordService service,RecordRepository repo,br.com.meuespaco.service.HouseAnalytics houseAnalytics){this.service=service;this.repo=repo;this.houseAnalytics=houseAnalytics;}
 @GetMapping("/records/{module}") Object list(@PathVariable String module,Principal p){return service.list(p.getName(),module);}
 @PostMapping("/records/{module}") Object save(@PathVariable String module,@Valid @RequestBody RecordInput input,Principal p){return service.save(p.getName(),module,input);}
 @DeleteMapping("/records/{module}/{id}") void delete(@PathVariable String module,@PathVariable UUID id,Principal p){service.delete(p.getName(),module,id);}
 @GetMapping("/history/{id}") Object history(@PathVariable UUID id,Principal p){return repo.history(p.getName(),id);}
 @GetMapping("/summary") Object summary(@RequestParam String month,Principal p){return service.summary(p.getName(),month);}
 @GetMapping("/house/summary") Object house(Principal p){return service.house(p.getName());}
 @GetMapping("/house/analytics") Object houseAnalytics(Principal p){return houseAnalytics.calculate(p.getName());}
 @GetMapping("/preferences") Object preferences(Principal p){return repo.preferences(p.getName());}
 @PutMapping("/preferences") Object preferences(@RequestBody Map<String,Object> input,Principal p){
  if(!Set.of("rose","sage").contains(input.get("theme")))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tema inválido.");
  try{ZoneId.of(input.get("timezone").toString());}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Fuso horário inválido.");}
  RecordService.number(input,"waterGoal");RecordService.number(input,"houseBudget");if(repo.json(input).length()>10000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);repo.preferences(p.getName(),input);return input;
 }
 @GetMapping("/export") Object export(Principal p){var data=new TreeMap<String,Object>();for(var module:RecordService.MODULES)data.put(module,repo.list(p.getName(),module));data.put("preferences",repo.preferences(p.getName()));data.put("exportedAt",Instant.now());return data;}
}
