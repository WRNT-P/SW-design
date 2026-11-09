package ocs.domain;

import java.io.Serializable;

public class InstrumentConfiguration implements Serializable {

    private String configId;
    private String requiredConfig;
    private boolean isReady;

    public InstrumentConfiguration(String configId, String requiredConfig) {
        this.configId = configId;
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
}
