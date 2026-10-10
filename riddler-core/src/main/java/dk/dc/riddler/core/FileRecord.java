package dk.dc.riddler.core;
import java.time.Instant;
import java.util.UUID;
public record FileRecord(UUID id, String name, String contentType, long size, String checksum, Instant createdAt) {}
