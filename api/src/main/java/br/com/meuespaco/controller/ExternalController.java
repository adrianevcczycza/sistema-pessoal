package br.com.meuespaco.controller;
import org.springframework.web.bind.annotation.*;
import br.com.meuespaco.service.ExternalService;
@RestController @RequestMapping("/api/external")
public class ExternalController {
 private final ExternalService service;public ExternalController(ExternalService service){this.service=service;}
 @GetMapping("/exchange") Object exchange(){return service.get("exchange");}
 @GetMapping("/news") Object news(){return service.get("news");}
}
