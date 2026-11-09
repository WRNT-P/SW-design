package ocs.storage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ocs.domain.InstrumentConfiguration;
import ocs.domain.ObservingProgram;
import ocs.domain.SciencePlan;
import ocs.domain.SoftErrorWarning;
import ocs.ports.SciencePlanRepository;

public class FileSciencePlanRepository implements SciencePlanRepository {

    public FileSciencePlanRepository() {
        if (Files.notExists(dataFile())) {
            InstrumentConfiguration configuration = new InstrumentConfiguration("GMOS:R400:r");
            ObservingProgram readyProgram = new ObservingProgram("OP-01", configuration);
            readyProgram.addWarning(new SoftErrorWarning("WARN-01", "High airmass may reduce image quality"));
            SciencePlan plan = new SciencePlan("SP-01", List.of(
                    readyProgram,
                    new ObservingProgram("OP-02", null)));
            Map<String, SciencePlan> plans = new HashMap<>();
            plans.put(plan.getPlanId(), plan);
            writeAll(plans);
        }
    }

    @Override
    public SciencePlan findById(String planId) {
        return readAll().get(planId);
    }

    @Override
    public void save(SciencePlan plan) {
        Map<String, SciencePlan> plans = readAll();
        plans.put(plan.getPlanId(), plan);
        writeAll(plans);
    }

    private static Path dataFile() {
        return Path.of("data", "science-plans.ser");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, SciencePlan> readAll() {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dataFile()))) {
            return (Map<String, SciencePlan>) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeAll(Map<String, SciencePlan> plans) {
        try {
            Files.createDirectories(dataFile().getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(dataFile()))) {
                out.writeObject(plans);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
