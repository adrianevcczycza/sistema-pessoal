package br.com.meuespaco.controller;
import java.security.Principal;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController
public class AuthController {
 private final JdbcTemplate db;private final PasswordEncoder encoder;
 public AuthController(JdbcTemplate db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
 @GetMapping("/health") Map<String,String> health(){return Map.of("status","ok");}
 @GetMapping("/api/auth/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @GetMapping("/api/auth/me") Map<String,String> me(Principal user){return Map.of("email",user.getName(),"name","Adriane");}
 @PostMapping("/api/auth/password") void password(@RequestBody Map<String,String> body,Principal user,HttpServletRequest req){
  var next=body.getOrDefault("newPassword","");var current=body.getOrDefault("currentPassword","");
  if(next.length()<14 || next.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A nova senha deve ter pelo menos 14 caracteres e até 72 bytes.");
  var hash=db.queryForObject("select password_hash from app_user where email=?",String.class,user.getName());
  if(!encoder.matches(current,hash))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha atual não confere.");
  db.update("update app_user set password_hash=? where email=?",encoder.encode(next),user.getName());
  db.update("delete from SPRING_SESSION where PRINCIPAL_NAME=?",user.getName());req.getSession().invalidate();
 }
}
