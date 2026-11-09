package ocs.ports;

public interface AuditRepository {

    void logAction(String action);
}
