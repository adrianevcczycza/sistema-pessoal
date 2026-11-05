package br.com.meuespaco.controller;
import java.security.Principal;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import br.com.meuespaco.dto.RecordInput;
import br.com.meuespaco.service.FinanceService;
@RestController @RequestMapping("/api/finance")
public class FinanceController {
 private final FinanceService service;public FinanceController(FinanceService service){this.service=service;}
 @PostMapping("/installments") Object installments(@Valid @RequestBody RecordInput input,Principal p){return service.installments(p.getName(),input);}
 @PostMapping("/recurring/{id}/next") Object next(@PathVariable UUID id,Principal p){return service.nextRecurring(p.getName(),id);}
 @GetMapping("/annual") Object annual(@RequestParam int year,Principal p){return service.annual(p.getName(),year);}
}
