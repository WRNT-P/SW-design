package ocs.storage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import ocs.domain.ObservingProgram;
import ocs.ports.ObservingProgramRepository;

public class FileObservingProgramRepository implements ObservingProgramRepository {

    public FileObservingProgramRepository() {
        if (Files.notExists(dataFile())) {
            writeAll(new HashMap<>());
        }
    }

    @Override
    public ObservingProgram findById(String programId) {
        return readAll().get(programId);
    }

    @Override
    public void save(ObservingProgram program) {
        Map<String, ObservingProgram> programs = readAll();
        programs.put(program.getProgramId(), program);
        writeAll(programs);
    }

    private static Path dataFile() {
        return Path.of("data", "programs.ser");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ObservingProgram> readAll() {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(dataFile()))) {
            return (Map<String, ObservingProgram>) in.readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeAll(Map<String, ObservingProgram> programs) {
        try {
            Files.createDirectories(dataFile().getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(dataFile()))) {
                out.writeObject(programs);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
