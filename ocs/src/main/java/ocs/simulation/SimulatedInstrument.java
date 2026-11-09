package ocs.simulation;

import ocs.ports.InstrumentGateway;

public class SimulatedInstrument implements InstrumentGateway {

    private String currentConfig;

    public SimulatedInstrument() {
        // The simulated instrument starts with no configuration applied.
    }

    @Override
    public String readCurrentConfig() {
        return currentConfig;
    }

    @Override
    public boolean applyConfig(String config) {
        currentConfig = config;
        return true;
    }
}
