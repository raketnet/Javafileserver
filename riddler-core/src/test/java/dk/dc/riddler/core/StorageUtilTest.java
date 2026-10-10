package dk.dc.riddler.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StorageUtilTest {
 @Test void sanitizesWindowsAndUnixPaths(){assertEquals("file.txt",StorageUtil.safeName("C:\\temp\\file.txt"));assertEquals("file.txt",StorageUtil.safeName("/tmp/file.txt"));}
 @Test void rejectsInvalidNames(){assertThrows(IllegalArgumentException.class,()->StorageUtil.safeName(".."));}
}
