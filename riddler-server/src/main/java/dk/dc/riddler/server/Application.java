package dk.dc.riddler.server;
import dk.dc.riddler.core.*;
import dk.dc.riddler.filesystem.FilesystemStorage;
import dk.dc.riddler.postgres.PostgresStorage;
import io.javalin.Javalin;
import java.nio.file.Path;
import java.io.*;
import java.util.*;
public final class Application {
 private Application(){}
 private static String env(String key,String fallback){return Optional.ofNullable(System.getenv(key)).filter(s->!s.isBlank()).orElse(fallback);}
 public static void main(String[] args)throws Exception {
  FileStorage storage=env("STORAGE_TYPE","filesystem").equalsIgnoreCase("postgres") ? new PostgresStorage(env("DB_URL","jdbc:postgresql://localhost:5432/riddler"),env("DB_USER","riddler"),env("DB_PASSWORD","riddler")) : new FilesystemStorage(Path.of(env("STORAGE_PATH","./data")));
  long maxBytes=Long.parseLong(env("MAX_UPLOAD_BYTES","26214400"));
  var app=Javalin.create(c->c.http.maxRequestSize=maxBytes+1048576);
  app.get("/health",ctx->ctx.json(Map.of("status","UP")));
  app.get("/health/live",ctx->ctx.json(Map.of("status","UP")));
  app.get("/health/ready",ctx->{storage.list(1,0);ctx.json(Map.of("status","READY"));});
  app.get("/openapi.yaml",ctx->{ctx.contentType("application/yaml");try(var in=Application.class.getResourceAsStream("/openapi.yaml")){if(in==null)throw new IOException("Missing OpenAPI");ctx.result(in.readAllBytes());}});
  app.get("/swagger",ctx->ctx.html(""" 
  <!doctype html><html><head><title>Riddler API</title><link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"></head><body><div id="swagger-ui"></div><script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script><script>SwaggerUIBundle({url:'/openapi.yaml',dom_id:'#swagger-ui'});</script></body></html>
  """));
  app.post("/api/v1/files",ctx->{var upload=ctx.uploadedFile("file");if(upload==null){ctx.status(400).json(Map.of("error","Missing multipart file"));return;}try(var in=upload.content()){var record=storage.save(upload.filename(),upload.contentType(),new LimitedInputStream(in,maxBytes));ctx.status(201).header("Location","/api/v1/files/"+record.id()).json(record);}});
  app.get("/api/v1/files",ctx->{int limit=Math.clamp(ctx.queryParamAsClass("limit",Integer.class).getOrDefault(50),1,100);int offset=Math.max(0,ctx.queryParamAsClass("offset",Integer.class).getOrDefault(0));ctx.json(storage.list(limit,offset));});
  app.get("/api/v1/files/{id}/metadata",ctx->{var id=UUID.fromString(ctx.pathParam("id"));var info=storage.find(id);if(info.isEmpty()){ctx.status(404);return;}ctx.json(info.get());});
  app.get("/api/v1/files/{id}",ctx->{var id=UUID.fromString(ctx.pathParam("id"));var info=storage.find(id);if(info.isEmpty()){ctx.status(404);return;}ctx.contentType("application/octet-stream");ctx.header("Content-Disposition","attachment; filename=\"download\"");ctx.header("X-Content-Type-Options","nosniff");ctx.result(storage.open(id));});
  app.delete("/api/v1/files/{id}",ctx->ctx.status(storage.delete(UUID.fromString(ctx.pathParam("id")))?204:404));
  app.exception(IllegalArgumentException.class,(e,ctx)->ctx.status(400).json(Map.of("error",e.getMessage())));
  app.exception(Exception.class,(e,ctx)->{System.err.println("Request failed: "+e);ctx.status(500).json(Map.of("error","Internal server error"));});
  Runtime.getRuntime().addShutdownHook(new Thread(()->{app.stop();try{storage.close();}catch(Exception e){System.err.println(e);}}));
  try {app.start(Integer.parseInt(env("PORT","7070")));}
  catch(RuntimeException e){try{storage.close();}catch(Exception closeError){e.addSuppressed(closeError);}throw e;}
 }
 private static final class LimitedInputStream extends FilterInputStream {private final long max;private long count;LimitedInputStream(InputStream in,long max){super(in);this.max=max;}public int read()throws IOException{int b=super.read();if(b!=-1&&++count>max)throw new IOException("Upload too large");return b;}public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,(int)Math.min(len,Math.max(1,max-count+1)));if(n>0&& (count+=n)>max)throw new IOException("Upload too large");return n;}}
}
