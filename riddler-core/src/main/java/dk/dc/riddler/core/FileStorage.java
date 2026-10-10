package dk.dc.riddler.core;
import java.io.*;
import java.util.*;
public interface FileStorage extends AutoCloseable {
 FileRecord save(String name, String contentType, InputStream input) throws IOException;
 Optional<FileRecord> find(UUID id) throws IOException;
 List<FileRecord> list(int limit, int offset) throws IOException;
 InputStream open(UUID id) throws IOException;
 boolean delete(UUID id) throws IOException;
 default void close() throws Exception {}
}
