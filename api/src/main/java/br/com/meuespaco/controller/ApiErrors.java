package br.com.meuespaco.controller;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> known(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Não foi possível realizar a operação.":e.getReason()));}
 @ExceptionHandler({MethodArgumentNotValidException.class,IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Confira os campos informados."));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> size(Exception e){return ResponseEntity.status(413).body(Map.of("message","O limite por arquivo é 10 MB."));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e){return ResponseEntity.status(500).body(Map.of("message","Não foi possível salvar. Tente novamente; seus dados não foram confirmados."));}
}
