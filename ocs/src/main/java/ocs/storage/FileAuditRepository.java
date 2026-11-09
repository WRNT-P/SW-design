package ocs.storage;

import ocs.ports.AuditRepository;

public class FileAuditRepository implements AuditRepository {

    public FileAuditRepository() {
        // Not implemented yet.
    }

    @Override
    public void logAction(String action) {
        throw new UnsupportedOperationException();
    }
}
