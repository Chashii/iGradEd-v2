package com.igraded.locker;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rules for what may go into the Locker, and for cleaning names that come from the browser.
 * Plain Java with no framework code, so it can be tested on its own.
 */
public final class FileRules {

    public static final String DEFAULT_FOLDER = "General";
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_FOLDER_LENGTH = 60;

    /**
     * Allowed file types, by extension. Web page and script types (html, svg, js, exe...) are left out on
     * purpose. Files are always downloaded as attachments and never shown inside the site.
     */
    private static final Set<String> ALLOWED = Collections.unmodifiableSet(new TreeSet<>(Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "png", "jpg", "jpeg", "gif", "zip")));

    private FileRules() {
    }

    public static Set<String> allowedExtensions() {
        return ALLOWED;
    }

    /** Extension in lower case without the dot, or "" when there is none. */
    public static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        // No dot, a dot at the very start (".pdf" is a hidden file, not a PDF) or at the very end: no extension
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowed(String fileName) {
        return ALLOWED.contains(extensionOf(fileName));
    }

    /**
     * Makes a name from the browser safe to show and to put in a ZIP: no folders, no path tricks
     * ("..", slashes), no control or special characters, not too long. The extension is kept.
     */
    public static String cleanFileName(String original) {
        String name = original == null ? "" : original;

        // Some browsers send a whole path. Keep only what follows the last slash.
        int cut = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (cut >= 0) {
            name = name.substring(cut + 1);
        }

        name = name.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_");
        name = name.replaceAll("\\s+", " ").trim();
        name = name.replaceAll("^\\.+", "");        // no hidden files, no ".."
        name = name.replaceAll("[. ]+$", "");        // Windows drops trailing dots and spaces
        if (name.isEmpty()) {
            return "file";
        }
        return limitLength(name, MAX_NAME_LENGTH);
    }

    /** Folder names are only labels (files are not stored in real folders), but they appear in ZIPs too. */
    public static String cleanFolder(String folder) {
        if (folder == null) {
            return DEFAULT_FOLDER;
        }
        String f = folder.replaceAll("[^\\p{L}\\p{N} _.&()'-]", "");
        f = f.replaceAll("\\s+", " ").trim();
        f = f.replaceAll("^\\.+", "");
        f = f.replaceAll("[. ]+$", "");
        if (f.isEmpty()) {
            return DEFAULT_FOLDER;
        }
        if (f.length() > MAX_FOLDER_LENGTH) {
            f = f.substring(0, MAX_FOLDER_LENGTH).trim();
        }
        return f;
    }

    /** 1536 becomes "1.5 KB", 2147483648 becomes "2 GB". */
    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = -1;
        do {
            value /= 1024;
            unit++;
        } while (value >= 1024 && unit < units.length - 1);

        if (value >= 100 || value == Math.rint(value)) {
            return String.format(Locale.ROOT, "%.0f %s", value, units[unit]);
        }
        return String.format(Locale.ROOT, "%.1f %s", value, units[unit]);
    }

    private static String limitLength(String name, int max) {
        if (name.length() <= max) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1) : "";
        if (ext.isEmpty() || ext.length() > 10) {
            return name.substring(0, max);
        }
        return name.substring(0, max - ext.length() - 1) + "." + ext;
    }
}
