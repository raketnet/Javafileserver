package dk.dc.riddler.filesystem;
import dk.dc.riddler.core.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;
public final class FilesystemStorage implements FileStorage {
 private final Path root;
 public FilesystemStorage(Path root) throws IOException {this.root=root.toAbsolutePath().normalize(); Files.createDirectories(this.root);
  try(var entries=Files.list(this.root)) {
   for(var file:entries.filter(f->f.getFileName().toString().startsWith("upload-") && f.getFileName().toString().endsWith(".tmp")).toList()) {
    // Only remove abandoned staging files on startup; run a single writer per directory.
    Files.deleteIfExists(file);
   }
  }}
 private Path data(UUID id){return root.resolve(id.toString()+".bin");}
 private Path meta(UUID id){return root.resolve(id.toString()+".properties");}
 public FileRecord save(String name,String contentType,InputStream input) throws IOException {
  name=StorageUtil.safeName(name); UUID id=UUID.randomUUID(); Path temp=Files.createTempFile(root,"upload-",".tmp");
  try {
   Files.copy(input,temp,StandardCopyOption.REPLACE_EXISTING); long size=Files.size(temp);
   String checksum; try(var in=Files.newInputStream(temp)){checksum=StorageUtil.sha256(in);}
   var record=new FileRecord(id,name,contentType==null?"application/octet-stream":contentType,size,checksum,Instant.now());
   var p=new Properties(); p.setProperty("name",record.name()); p.setProperty("type",record.contentType()); p.setProperty("size",Long.toString(size)); p.setProperty("checksum",checksum); p.setProperty("created",record.createdAt().toString());
   Files.move(temp,data(id),StandardCopyOption.ATOMIC_MOVE);
   Path metaTemp=Files.createTempFile(root,"upload-",".tmp");
   try {
    try(var out=Files.newOutputStream(metaTemp)){p.store(out,"Riddler metadata");}
    Files.move(metaTemp,meta(id),StandardCopyOption.ATOMIC_MOVE);
   } finally {Files.deleteIfExists(metaTemp);}
   return record;
  } catch(Exception e){Files.deleteIfExists(temp);Files.deleteIfExists(data(id));Files.deleteIfExists(meta(id));if(e instanceof IOException io)throw io;throw new IOException(e);}
 }
 public Optional<FileRecord> find(UUID id) throws IOException {
  if(!Files.isRegularFile(meta(id)) || !Files.isRegularFile(data(id))) return Optional.empty();
  var p=new Properties(); try(var in=Files.newInputStream(meta(id))){p.load(in);}
  return Optional.of(new FileRecord(id,p.getProperty("name"),p.getProperty("type"),Long.parseLong(p.getProperty("size")),p.getProperty("checksum"),Instant.parse(p.getProperty("created"))));
 }
 public List<FileRecord> list(int limit,int offset) throws IOException {
  try(var stream=Files.list(root)) {var paths=stream.filter(p->p.getFileName().toString().endsWith(".properties")).toList();
   var records=new ArrayList<FileRecord>(); for(var p:paths){try {var id=UUID.fromString(p.getFileName().toString().replace(".properties",""));find(id).ifPresent(records::add);}catch(IllegalArgumentException ignored){}}
   return records.stream().sorted(Comparator.comparing(FileRecord::createdAt).reversed().thenComparing(FileRecord::id)).skip(offset).limit(limit).toList(); }
 }
 public InputStream open(UUID id) throws IOException {if(find(id).isEmpty())throw new FileNotFoundException(id.toString());return Files.newInputStream(data(id));}
 public boolean delete(UUID id) throws IOException {if(find(id).isEmpty())return false;Files.deleteIfExists(meta(id));Files.deleteIfExists(data(id));return true;}
}
