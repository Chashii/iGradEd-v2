package com.igraded.locker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Keeps the file bytes on disk: one folder per teacher, and every file saved under a random name.
 * The name the teacher gave it lives only in the database, so nothing a user types ever becomes a path.
 *
 * Plain Java, no framework code. To move to cloud storage later, replace this class and keep the same methods.
 */
public class LockerStorage {

    /** What save() returns. */
    public record Stored(String storedName, long size) {
    }

    private static final Pattern STORED_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private final Path root;

    public LockerStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /** Copies the stream to a new random file in the owner's folder. Returns its random name and byte size. */
    public Stored save(long ownerId, InputStream in) throws IOException {
        Path dir = ownerDir(ownerId);
        Files.createDirectories(dir);

        String storedName = UUID.randomUUID().toString();
        Path target = dir.resolve(storedName);
        try {
            long size = Files.copy(in, target);
            return new Stored(storedName, size);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);   // never leave a half-written file behind
            throw e;
        }
    }

    /** Where a stored file lives. Refuses anything that is not a name we generated. */
    public Path resolve(long ownerId, String storedName) {
        if (storedName == null || !STORED_NAME.matcher(storedName).matches()) {
            throw new IllegalArgumentException("Invalid stored file name");
        }
        return ownerDir(ownerId).resolve(storedName);
    }

    /** Returns true if a file was removed. */
    public boolean delete(long ownerId, String storedName) throws IOException {
        return Files.deleteIfExists(resolve(ownerId, storedName));
    }

    private Path ownerDir(long ownerId) {
        return root.resolve(Long.toString(ownerId));
    }
}
