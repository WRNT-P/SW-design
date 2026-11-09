package ocs.ports;

import ocs.domain.ObservingProgram;

public interface ObservingProgramRepository {

    ObservingProgram findById(String programId);

    void save(ObservingProgram program);
}
