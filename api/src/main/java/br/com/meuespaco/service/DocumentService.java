package br.com.meuespaco.service;
import java.net.URI;
import java.util.*;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import br.com.meuespaco.repository.RecordRepository;
@Service
public class DocumentService {
 private final RecordRepository repo;private final S3Client s3;private final String bucket;
 private static final Map<String,String> TYPES=Map.of("application/pdf","pdf","image/png","png","image/jpeg","jpg","image/webp","webp");
 public DocumentService(RecordRepository repo,@Value("${app.s3.endpoint}") String endpoint,@Value("${app.s3.region}") String region,@Value("${app.s3.bucket}") String bucket,@Value("${app.s3.access-key}") String access,@Value("${app.s3.secret-key}") String secret){
  this.repo=repo;this.bucket=bucket;
  if(bucket.isBlank() || access.isBlank() || secret.isBlank())s3=null;
  else {var builder=S3Client.builder().region(Region.of(region)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());if(!endpoint.isBlank())builder.endpointOverride(URI.create(endpoint));s3=builder.build();}
 }
 public boolean configured(){return s3!=null;}
 private void ready(){if(s3==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O armazenamento privado ainda não foi configurado.");}
 @Transactional public Object upload(String owner,MultipartFile file,Map<String,Object> metadata,UUID id) throws Exception {
  ready();repo.lockOwner(owner);RecordService.required(metadata,"title");RecordService.number(metadata,"hours");
  var existing=repo.list(owner,"documents").stream().filter(r->r.get("id").equals(id.toString())).findFirst();if(existing.isPresent())return existing.get();
  if(file.isEmpty() || file.getSize()>10*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Envie um arquivo de até 10 MB.");
  byte[] bytes=file.getBytes();String mime=new Tika().detect(bytes);if(!TYPES.containsKey(mime))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Formatos permitidos: PDF, PNG, JPEG e WebP.");
  if(metadata.get("purchaseId")!=null && !metadata.get("purchaseId").toString().isBlank())repo.find(owner,"purchases",UUID.fromString(metadata.get("purchaseId").toString()));
  String key="private/"+id+"."+TYPES.get(mime);s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(mime).serverSideEncryption(ServerSideEncryption.AES256).build(),RequestBody.fromBytes(bytes));
  var data=new LinkedHashMap<>(metadata);data.put("key",key);data.put("mime",mime);data.put("size",file.getSize());data.put("filename","documento."+TYPES.get(mime));
  try{return repo.save(owner,"documents",id,data,0);}catch(RuntimeException e){s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());throw e;}
 }
 public Map<String,Object> metadata(String owner,UUID id){return RecordService.data(repo.find(owner,"documents",id));}
 @Transactional public Object update(String owner,UUID id,Map<String,Object> fields,long version){repo.lockOwner(owner);var old=new LinkedHashMap<>(metadata(owner,id));RecordService.required(fields,"title");RecordService.number(fields,"hours");for(var key:List.of("title","category","institution","course","hours","date","deadline","tags","purchaseId","projectId","noteId","description"))if(fields.containsKey(key))old.put(key,fields.get(key));return repo.save(owner,"documents",id,old,version);}
 public byte[] download(String owner,UUID id){ready();var d=metadata(owner,id);return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(d.get("key").toString()).build()).asByteArray();}
 @Transactional public void delete(String owner,UUID id){ready();repo.lockOwner(owner);var d=metadata(owner,id);s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(d.get("key").toString()).build());repo.delete(owner,"documents",id);}
}
