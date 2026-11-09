package ocs.simulation;

import ocs.ports.TelescopeGateway;

public class SimulatedTelescope implements TelescopeGateway {

    public SimulatedTelescope() {
        // No external telescope is connected in the prototype.
    }

    @Override
    public boolean startObservation() {
        return true;
    }
}
