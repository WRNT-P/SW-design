package ocs.simulation;

import ocs.ports.WeatherGateway;

public class SimulatedWeather implements WeatherGateway {

    public SimulatedWeather() {
        // No external weather system is connected in the prototype.
    }

    @Override
    public boolean checkSafeConditions() {
        return true;
    }
}
