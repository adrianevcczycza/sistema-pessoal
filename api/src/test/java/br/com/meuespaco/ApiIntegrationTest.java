package br.com.meuespaco;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import br.com.meuespaco.service.RecordService;
import br.com.meuespaco.repository.RecordRepository;
import br.com.meuespaco.dto.RecordInput;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc(printOnlyOnFailure=false, print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class ApiIntegrationTest {
 static final String OWNER="adriane.wegg@gmail.com";
 static final String TEST_PASSWORD=UUID.randomUUID().toString();
 static EmbeddedPostgres postgres;
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
  postgres=EmbeddedPostgres.builder().setPort(0).start();
  registry.add("spring.datasource.url",()->postgres.getJdbcUrl("postgres","postgres"));registry.add("spring.datasource.username",()->"postgres");registry.add("spring.datasource.password",()->"");registry.add("app.initial-password",()->TEST_PASSWORD);registry.add("server.servlet.session.cookie.secure",()->false);
 }
 @Autowired MockMvc mvc;@Autowired RecordService service;@Autowired RecordRepository repo;@Autowired JdbcTemplate db;
 @Autowired br.com.meuespaco.service.FinanceService finance;
 @Autowired br.com.meuespaco.service.ImportService importer;
 @BeforeEach void clean(){db.update("delete from record_history");db.update("delete from records");db.update("delete from preferences");}
 @AfterAll static void stop() throws Exception {if(postgres!=null)postgres.close();}
 RecordInput input(Map<String,Object> d){return new RecordInput(UUID.randomUUID(),d,0);}
 @Test void anonymousCannotReadOrDownload() throws Exception {mvc.perform(get("/api/records/tasks")).andExpect(status().isUnauthorized());mvc.perform(get("/api/documents/"+UUID.randomUUID()+"/download")).andExpect(status().isUnauthorized());}
 @Test void loginRequiresCsrfAndCorrectCredentials() throws Exception {
  mvc.perform(post("/api/auth/login").param("email",OWNER).param("password",TEST_PASSWORD)).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/login").with(csrf()).param("email",OWNER).param("password","incorrect")).andExpect(status().isUnauthorized());
  var result=mvc.perform(post("/api/auth/login").with(csrf()).param("email",OWNER).param("password",TEST_PASSWORD)).andExpect(status().isOk()).andReturn();
  var session=result.getResponse().getCookie("SESSION");
  mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(OWNER));
  mvc.perform(post("/api/auth/logout").cookie(session).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
 }
 @Test @WithMockUser(username=OWNER) void taskPersistsAndKeepsHistory() throws Exception {
  UUID id=UUID.randomUUID();String body=repo.json(new RecordInput(id,Map.of("title","Estudar","done",false),0));
  mvc.perform(post("/api/records/tasks").contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/records/tasks").with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(post("/api/records/tasks").with(csrf()).contentType("application/json").content(repo.json(new RecordInput(id,Map.of("title","Estudar","done",true),0)))).andExpect(status().isOk());
  mvc.perform(get("/api/records/tasks")).andExpect(jsonPath("$[0].data.done").value(true));assertEquals(2,repo.history(OWNER,id).size());
  mvc.perform(post("/api/records/tasks").with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 }
 @Test void financialSummaryExcludesTransfersAndForecasts(){
  for(var entry:List.of(Map.<String,Object>of("title","Salário","amount","1000.10","type","Receita","date","2026-11-01","paid",true),Map.<String,Object>of("title","Mercado","amount","100.05","type","Despesa","date","2026-11-02","paid",true),Map.<String,Object>of("title","Transferência","amount","200","type","Transferência","date","2026-11-02","paid",true),Map.<String,Object>of("title","Conta futura","amount","50","type","Despesa","date","2026-11-20","paid",false)))service.save(OWNER,"transactions",input(entry));
  var summary=service.summary(OWNER,"2026-11");assertEquals(0,new BigDecimal("900.05").compareTo((BigDecimal)summary.get("balance")));assertEquals(0,new BigDecimal("50").compareTo((BigDecimal)summary.get("planned")));
 }
 @Test void partialPurchaseAndLinkedExpenseAreAtomicAndReversible(){
  var house=input(Map.of("title","Cadeiras","needed",4,"owned",1,"gifted",1,"price","100","room","Sala"));service.save(OWNER,"house",house);
  var purchase=input(Map.of("title","Cadeira","houseId",house.id().toString(),"quantity",1,"amount","90","freight","10","date","2026-11-05","createExpense",true));
  var saved=service.save(OWNER,"purchases",purchase);assertEquals(1,repo.list(OWNER,"transactions").size());
  service.save(OWNER,"purchases",new RecordInput(purchase.id(),purchase.data(),RecordService.version(saved)));assertEquals(1,repo.list(OWNER,"transactions").size());
  var summary=service.house(OWNER);assertEquals(0,new BigDecimal("100").compareTo((BigDecimal)summary.get("spent")));assertEquals(0,new BigDecimal("100").compareTo((BigDecimal)summary.get("pending")));
  var invalid=new HashMap<>(purchase.data());invalid.put("quantity",3);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.save(OWNER,"purchases",input(invalid)));assertEquals(1,repo.list(OWNER,"purchases").size());
  service.delete(OWNER,"purchases",purchase.id());assertTrue(repo.list(OWNER,"transactions").isEmpty());assertEquals(0,((BigDecimal)service.house(OWNER).get("spent")).signum());
 }
 @Test void giftsResolveWithoutExpenseAndIgnoredItemsDoNotCount(){
  service.save(OWNER,"house",input(Map.of("title","Mesa","needed",1,"gifted",1)));service.save(OWNER,"house",input(Map.of("title","TV","needed",1,"notNeeded",true)));
  var summary=service.house(OWNER);assertEquals(1,summary.get("active"));assertEquals(1,summary.get("resolved"));assertEquals(100L,summary.get("progress"));assertTrue(repo.list(OWNER,"transactions").isEmpty());
 }
 @Test void rejectsNegativeAmountsAndCrossOwnerUpdates(){
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.save(OWNER,"house",input(Map.of("title","Inválido","needed",-1))));
  var item=input(Map.of("title","Privado"));service.save(OWNER,"notes",item);assertThrows(org.springframework.web.server.ResponseStatusException.class,()->repo.find("other@example.org","notes",item.id()));
 }
 @Test @WithMockUser(username=OWNER) void integrationsAndDocumentsReportMissingConfiguration() throws Exception {mvc.perform(get("/api/external/exchange")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("not_configured"));mvc.perform(get("/api/documents/status")).andExpect(jsonPath("$.configured").value(false));}
 @Test void installmentsPreserveTotalAndDoNotDuplicate(){
  var input=input(Map.of("title","Parcelado","type","Despesa","date","2026-11-05","amount","100.00","installments",3));finance.installments(OWNER,input);finance.installments(OWNER,input);
  var rows=repo.list(OWNER,"transactions");assertEquals(3,rows.size());var sum=rows.stream().map(RecordService::data).map(d->RecordService.number(d,"amount")).reduce(BigDecimal.ZERO,BigDecimal::add);assertEquals(0,new BigDecimal("100").compareTo(sum));
 }
 @Test void recurringTaskCreatesOneNextOccurrenceEvenAfterReopen(){
  var input=input(Map.of("title","Ler","date","2026-11-05","recurrence","Diária","done",false));var r=service.save(OWNER,"tasks",input);var done=new HashMap<>(input.data());done.put("done",true);r=service.save(OWNER,"tasks",new RecordInput(input.id(),done,RecordService.version(r)));r=service.save(OWNER,"tasks",new RecordInput(input.id(),input.data(),RecordService.version(r)));service.save(OWNER,"tasks",new RecordInput(input.id(),done,RecordService.version(r)));assertEquals(2,repo.list(OWNER,"tasks").size());
 }
 @Test void richTextIsSanitizedBeforePersistence(){
  var r=service.save(OWNER,"notes",input(Map.of("title","Nota","html","<p>Texto<script>alert(1)</script><a href='javascript:alert(1)'>link</a><img src=x onerror=alert(1)></p>")));String html=RecordService.data(r).get("html").toString();assertFalse(html.contains("script"));assertFalse(html.contains("onerror"));assertTrue(html.contains("Texto"));
 }
 @Test @SuppressWarnings("unchecked") void synthetic110ItemWorkbookRequiresConfirmationAndCannotDuplicate() throws Exception {
  byte[] bytes;try(var book=new org.apache.poi.xssf.usermodel.XSSFWorkbook();var out=new java.io.ByteArrayOutputStream()){
   var sheet=book.createSheet("Checklist");var header=sheet.createRow(0);String[] names={"Ambiente","Item ou kit","Quantidade","Teto unitário","Prioridade"};for(int c=0;c<names.length;c++)header.createCell(c).setCellValue(names[c]);
   for(int i=1;i<=110;i++){var row=sheet.createRow(i);row.createCell(0).setCellValue("Ambiente de teste");row.createCell(1).setCellValue("Item sintético "+i);row.createCell(2).setCellValue(1);row.createCell(3).setCellValue(10);row.createCell(4).setCellValue("Essencial");}
   var links=book.createSheet("Links completos");links.createRow(0).createCell(0).setCellValue("Item");links.getRow(0).createCell(1).setCellValue("Amazon");links.createRow(1).createCell(0).setCellValue("Item sintético 1");links.getRow(1).createCell(1).setCellValue("https://example.org/pesquisa");book.write(out);bytes=out.toByteArray();
  }
  var preview=(Map<String,Object>)importer.preview(OWNER,new org.springframework.mock.web.MockMultipartFile("file","teste.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",bytes));assertEquals(110,preview.get("count"));assertTrue(repo.list(OWNER,"house").isEmpty());var rows=(List<Map<String,Object>>)preview.get("items");var inputs=rows.stream().map(r->new RecordInput((UUID)r.get("id"),(Map<String,Object>)r.get("data"),0)).toList();importer.confirm(OWNER,inputs);assertEquals(110,repo.list(OWNER,"house").size());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->importer.confirm(OWNER,inputs));assertEquals(110,repo.list(OWNER,"house").size());
 }
}
