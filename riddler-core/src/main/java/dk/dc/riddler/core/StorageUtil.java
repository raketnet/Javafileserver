package dk.dc.riddler.core;
import java.security.*;
import java.io.*;
import java.util.HexFormat;
public final class StorageUtil {
 private StorageUtil() {}
 public static String safeName(String name) {
  if(name == null) throw new IllegalArgumentException("Missing filename");
  String normalized = name.replace('\\','/');
  normalized = normalized.substring(normalized.lastIndexOf('/')+1).trim();
  if(normalized.isEmpty() || normalized.equals(".") || normalized.equals("..") || normalized.chars().anyMatch(c -> c < 32)) throw new IllegalArgumentException("Invalid filename");
  return normalized;
 }
 public static String sha256(InputStream input) throws IOException {
  try { var digest=MessageDigest.getInstance("SHA-256"); byte[] buf=new byte[8192]; int n; while((n=input.read(buf))!=-1) digest.update(buf,0,n); return HexFormat.of().formatHex(digest.digest()); }
  catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
 }
}
