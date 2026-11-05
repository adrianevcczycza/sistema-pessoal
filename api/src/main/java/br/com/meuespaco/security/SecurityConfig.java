package br.com.meuespaco.security;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
@Configuration
public class SecurityConfig {
 public static final String EMAIL="adriane.wegg@gmail.com";
 @Bean PasswordEncoder encoder(){return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService users(JdbcTemplate jdbc){return email -> jdbc.query("select email,password_hash from app_user where email=?",(rs,n)-> User.withUsername(rs.getString(1)).password(rs.getString(2)).roles("OWNER").build(),email).stream().findFirst().orElseThrow(()->new UsernameNotFoundException("Acesso inválido"));}
 @Bean ApplicationRunner provision(JdbcTemplate jdbc,PasswordEncoder encoder,@Value("${app.initial-password}") String password){return args->{
  if(jdbc.queryForObject("select count(*) from app_user",Integer.class)==0){
   if(password.length()<14 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalStateException("Configure INITIAL_PASSWORD com pelo menos 14 caracteres e até 72 bytes para criar a conta privada.");
   jdbc.update("insert into app_user(email,password_hash) values (?,?)",EMAIL,encoder.encode(password));
  }
 };}
 @Bean SecurityFilterChain security(HttpSecurity http,@Value("${app.origin}") String origin) throws Exception {
  http.cors(c->c.configurationSource(request->{var cfg=new CorsConfiguration();cfg.setAllowedOrigins(List.of(origin.split(",")));cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cfg.setAllowedHeaders(List.of("Content-Type","X-CSRF-TOKEN"));cfg.setAllowCredentials(true);return cfg;}));
  http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/health").permitAll().anyRequest().authenticated());
  http.addFilterBefore(new LoginRateLimit(),UsernamePasswordAuthenticationFilter.class);
  http.formLogin(f->f.loginProcessingUrl("/api/auth/login").usernameParameter("email").successHandler((req,res,auth)->{res.setContentType("application/json");res.getWriter().write("{\"authenticated\":true}");}).failureHandler((req,res,e)->res.sendError(401,"E-mail ou senha inválidos.")));
  http.logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,auth)->res.setStatus(204)).deleteCookies("SESSION"));
  http.exceptionHandling(e->e.authenticationEntryPoint((req,res,x)->res.sendError(401)).accessDeniedHandler((req,res,x)->res.sendError(403)));
  http.headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
  return http.build();
 }
}
