package ocs.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import ocs.ports.AuditRepository;

public class FileAuditRepository implements AuditRepository {

    public FileAuditRepository() {
        try {
            Files.createDirectories(Path.of("data"));
            if (Files.notExists(auditFile())) {
                Files.createFile(auditFile());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void logAction(String action) {
        try {
            Files.writeString(auditFile(), action + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path auditFile() {
        return Path.of("data", "audit.log");
    }
}
