package br.com.meuespaco;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
/** Local launcher only: test-scope dependency, never packaged in the production JAR. */
public class LocalDevelopment {
 public static void main(String[] args) throws Exception {
  Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();if(root.getFileName().toString().equals("api"))root=root.getParent();
  Path local=root.resolve(".local");Files.createDirectories(local);Path passwordFile=local.resolve("initial-password.txt");
  if(!Files.exists(passwordFile)){byte[] bytes=new byte[24];new SecureRandom().nextBytes(bytes);Files.writeString(passwordFile,Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),StandardOpenOption.CREATE_NEW);}
  var postgres=EmbeddedPostgres.builder().setPort(54329).setDataDirectory(local.resolve("postgres")).setCleanDataDirectory(false).setServerConfig("listen_addresses","127.0.0.1").start();
  System.setProperty("spring.datasource.url",postgres.getJdbcUrl("postgres","postgres"));System.setProperty("spring.datasource.username","postgres");System.setProperty("spring.datasource.password","");System.setProperty("server.address","127.0.0.1");System.setProperty("server.port","8181");System.setProperty("server.servlet.session.cookie.secure","false");System.setProperty("app.initial-password",Files.readString(passwordFile).trim());System.setProperty("app.origin","http://localhost:3100,http://127.0.0.1:3100");
  Runtime.getRuntime().addShutdownHook(new Thread(()->{try{postgres.close();}catch(Exception ignored){}}));
  var context=org.springframework.boot.SpringApplication.run(Application.class,args);
  context.getBean(org.springframework.jdbc.core.JdbcTemplate.class).execute("SET log_statement='none'");
  new java.util.concurrent.CountDownLatch(1).await();
 }
}
