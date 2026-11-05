package br.com.meuespaco.controller;
import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.meuespaco.dto.RecordInput;
import br.com.meuespaco.service.ImportService;
@RestController @RequestMapping("/api/house/import")
public class ImportController {
 private final ImportService service;public ImportController(ImportService service){this.service=service;}
 @PostMapping("/preview") Object preview(@RequestParam MultipartFile file,Principal p) throws Exception{return service.preview(p.getName(),file);}
 @PostMapping("/confirm") Object confirm(@Valid @RequestBody List<@Valid RecordInput> rows,Principal p){return service.confirm(p.getName(),rows);}
}
