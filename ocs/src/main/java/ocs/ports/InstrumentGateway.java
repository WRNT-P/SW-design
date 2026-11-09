package ocs.ports;

public interface InstrumentGateway {

    String readCurrentConfig();

    boolean applyConfig(String config);
}
