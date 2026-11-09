package ocs.domain;

import java.io.Serializable;

public class InstrumentConfiguration implements Serializable {

    private final String requiredConfig;
    private boolean isReady;

    public InstrumentConfiguration(String requiredConfig) {
        this.requiredConfig = requiredConfig;
    }

    public String getRequiredConfig() {
        return requiredConfig;
    }

    public boolean matches(String actualConfig) {
        return requiredConfig.equals(actualConfig);
    }

    public void markReady() {
        isReady = true;
    }

    public boolean isInstrumentReady() {
        return isReady;
    }
}
