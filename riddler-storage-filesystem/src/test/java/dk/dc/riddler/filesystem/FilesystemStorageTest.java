package dk.dc.riddler.filesystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class FilesystemStorageTest {
 @TempDir Path directory;
 @Test void uploadReadListDelete()throws Exception {var store=new FilesystemStorage(directory);var data="hello".getBytes(StandardCharsets.UTF_8);var file=store.save("hello.txt","text/plain",new ByteArrayInputStream(data));assertEquals(5,file.size());assertEquals(64,file.checksum().length());assertEquals(1,store.list(10,0).size());try(var in=store.open(file.id())){assertArrayEquals(data,in.readAllBytes());}assertTrue(store.delete(file.id()));assertTrue(store.find(file.id()).isEmpty());}
}
