package dk.dc.riddler.postgres;
import dk.dc.riddler.core.*;
import com.zaxxer.hikari.*;
import org.flywaydb.core.Flyway;
import java.io.*;
import java.sql.*;
import java.util.*;
public final class PostgresStorage implements FileStorage {
 private final HikariDataSource ds;
 public PostgresStorage(String url,String user,String password) {
  var config=new HikariConfig();config.setJdbcUrl(url);config.setUsername(user);config.setPassword(password);config.setMaximumPoolSize(10);
  config.setConnectionTimeout(10000);
  config.setValidationTimeout(3000);
  config.setMaxLifetime(1800000);
  ds=new HikariDataSource(config);
  try { Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate(); }
  catch(RuntimeException e) { ds.close(); throw e; }
 }
 public FileRecord save(String name,String type,InputStream input) throws IOException {
  name=StorageUtil.safeName(name);if(type==null)type="application/octet-stream";
  // PostgreSQL bytea requires driver-side buffering. Stream to a bounded temporary file first.
  PathHolder holder=null;
  try {holder=new PathHolder(input);var id=UUID.randomUUID();try(var c=ds.getConnection();var ps=c.prepareStatement("INSERT INTO files(id,name,content_type,data) VALUES(?,?,?,?)")){
   ps.setObject(1,id);ps.setString(2,name);ps.setString(3,type);try(var in=holder.open()){ps.setBinaryStream(4,in,holder.size());ps.executeUpdate();}}
   return find(id).orElseThrow();
  }catch(SQLException e){throw new IOException(e);}finally{if(holder!=null)holder.close();}
 }
 private static final class PathHolder implements AutoCloseable {private final java.nio.file.Path path;PathHolder(InputStream in)throws IOException{path=java.nio.file.Files.createTempFile("riddler-pg-",".tmp");try{java.nio.file.Files.copy(in,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){close();throw e;}}long size()throws IOException{return java.nio.file.Files.size(path);}InputStream open()throws IOException{return java.nio.file.Files.newInputStream(path);}public void close()throws IOException{java.nio.file.Files.deleteIfExists(path);}}
 private FileRecord row(ResultSet rs)throws SQLException{return new FileRecord((UUID)rs.getObject("id"),rs.getString("name"),rs.getString("content_type"),rs.getLong("size"),rs.getString("checksum"),rs.getTimestamp("created_at").toInstant());}
 private static final String FIELDS="id,name,content_type,octet_length(data) AS size,encode(sha256(data),'hex') AS checksum,created_at";
 public Optional<FileRecord> find(UUID id)throws IOException {try(var c=ds.getConnection();var ps=c.prepareStatement("SELECT "+FIELDS+" FROM files WHERE id=?")){ps.setObject(1,id);try(var rs=ps.executeQuery()){return rs.next()?Optional.of(row(rs)):Optional.empty();}}catch(SQLException e){throw new IOException(e);}}
 public List<FileRecord> list(int limit,int offset)throws IOException {try(var c=ds.getConnection();var ps=c.prepareStatement("SELECT "+FIELDS+" FROM files ORDER BY created_at DESC,id LIMIT ? OFFSET ?")){ps.setInt(1,limit);ps.setInt(2,offset);var result=new ArrayList<FileRecord>();try(var rs=ps.executeQuery()){while(rs.next())result.add(row(rs));}return result;}catch(SQLException e){throw new IOException(e);}}
 public InputStream open(UUID id)throws IOException {
  Connection c=null; PreparedStatement ps=null; ResultSet rs=null;
  try {
   c=ds.getConnection(); ps=c.prepareStatement("SELECT data FROM files WHERE id=?"); ps.setObject(1,id);
   rs=ps.executeQuery();
   if(!rs.next())throw new FileNotFoundException(id.toString());
   InputStream data=rs.getBinaryStream(1);
   final Connection connection=c; final PreparedStatement statement=ps; final ResultSet result=rs;
   return new FilterInputStream(data) {
    private boolean closed;
    @Override public void close()throws IOException {
     if(closed)return; closed=true;
     IOException failure=null;
     try{super.close();}catch(IOException e){failure=e;}
     try{result.close();}catch(SQLException e){if(failure==null)failure=new IOException(e);else failure.addSuppressed(e);}
     try{statement.close();}catch(SQLException e){if(failure==null)failure=new IOException(e);else failure.addSuppressed(e);}
     try{connection.close();}catch(SQLException e){if(failure==null)failure=new IOException(e);else failure.addSuppressed(e);}
     if(failure!=null)throw failure;
    }
   };
  }catch(Exception e){
   if(rs!=null)try{rs.close();}catch(SQLException ignored){}
   if(ps!=null)try{ps.close();}catch(SQLException ignored){}
   if(c!=null)try{c.close();}catch(SQLException ignored){}
   if(e instanceof IOException io)throw io;
   throw new IOException(e);
  }
 }
 public boolean delete(UUID id)throws IOException {try(var c=ds.getConnection();var ps=c.prepareStatement("DELETE FROM files WHERE id=?")){ps.setObject(1,id);return ps.executeUpdate()>0;}catch(SQLException e){throw new IOException(e);}}
 public void close(){ds.close();}
}
