package br.com.meuespaco.controller;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.meuespaco.service.DocumentService;
import br.com.meuespaco.repository.RecordRepository;
@RestController @RequestMapping("/api/documents")
public class DocumentsController {
 private final DocumentService service;private final RecordRepository repo;
 public DocumentsController(DocumentService service,RecordRepository repo){this.service=service;this.repo=repo;}
 @GetMapping("/status") Object status(){return Map.of("configured",service.configured());}
 @PutMapping("/{id}") Object update(@PathVariable UUID id,@jakarta.validation.Valid @RequestBody br.com.meuespaco.dto.RecordInput input,Principal p){return service.update(p.getName(),id,input.data(),input.version());}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Object upload(@RequestParam MultipartFile file,@RequestParam String metadata,@RequestParam UUID id,Principal p) throws Exception{return service.upload(p.getName(),file,repo.parse(metadata),id);}
 @GetMapping("/{id}/download") ResponseEntity<byte[]> download(@PathVariable UUID id,Principal p){var d=service.metadata(p.getName(),id);return ResponseEntity.ok().header("Cache-Control","no-store").header("Content-Disposition","attachment; filename=\""+d.get("filename")+"\"").contentType(MediaType.parseMediaType(d.get("mime").toString())).body(service.download(p.getName(),id));}
 @DeleteMapping("/{id}") void delete(@PathVariable UUID id,Principal p){service.delete(p.getName(),id);}
}
