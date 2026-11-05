package br.com.meuespaco.security;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
/** Single-account global limiter; does not trust spoofable proxy headers. */
public class LoginRateLimit extends OncePerRequestFilter {
 private final ArrayDeque<Instant> attempts=new ArrayDeque<>();
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  if(req.getMethod().equals("POST") && req.getServletPath().equals("/api/auth/login")){
   synchronized(attempts){var now=Instant.now();while(!attempts.isEmpty() && attempts.peek().isBefore(now.minusSeconds(300)))attempts.remove();if(attempts.size()>=10){res.setHeader("Retry-After","300");res.sendError(429);return;}attempts.add(now);}
  }chain.doFilter(req,res);
 }
}
