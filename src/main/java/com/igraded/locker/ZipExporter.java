package com.igraded.locker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Writes files into a ZIP as they are read, so a big export never has to fit in memory. */
public final class ZipExporter {

    /** One file to include: the path it gets inside the ZIP, and where it is on disk. */
    public record Entry(String zipPath, Path file) {
    }

    private ZipExporter() {
    }

    /** Closes the output stream when done. Files that have gone missing from disk are skipped. */
    public static void write(OutputStream out, List<Entry> entries) throws IOException {
        Set<String> used = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                if (!Files.isRegularFile(entry.file())) {
                    continue;
                }
                ZipEntry zipEntry = new ZipEntry(uniquePath(entry.zipPath(), used));
                zipEntry.setTime(Files.getLastModifiedTime(entry.file()).toMillis());
                zip.putNextEntry(zipEntry);
                Files.copy(entry.file(), zip);
                zip.closeEntry();
            }
        }
    }

    /** Two files with the same name in a folder become "x.pdf" and "x (2).pdf". */
    static String uniquePath(String path, Set<String> used) {
        String candidate = path;
        int n = 2;
        while (!used.add(candidate.toLowerCase(Locale.ROOT))) {
            int dot = path.lastIndexOf('.');
            int slash = path.lastIndexOf('/');
            boolean hasExt = dot > slash;
            String base = hasExt ? path.substring(0, dot) : path;
            String ext = hasExt ? path.substring(dot) : "";
            candidate = base + " (" + n + ")" + ext;
            n++;
        }
        return candidate;
    }
}
