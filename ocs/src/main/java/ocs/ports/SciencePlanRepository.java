package ocs.ports;

import ocs.domain.SciencePlan;

public interface SciencePlanRepository {

    SciencePlan findById(String planId);

    void save(SciencePlan plan);
}
