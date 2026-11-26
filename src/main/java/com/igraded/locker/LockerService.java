package com.igraded.locker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.igraded.user.AppUser;
import com.igraded.user.UserRepository;

/**
 * Everything the Locker does. Every method takes the owner's id and only ever touches that owner's files.
 */
@Service
public class LockerService {

    private static final Logger log = LoggerFactory.getLogger(LockerService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** What the Locker page shows. */
    public record Overview(UsageView usage, List<FolderView> folders, List<FileView> files,
                           int totalCount, String selectedFolder) {
    }

    /** The outcome of an upload: how many files were saved, and why each of the others was not. */
    public record UploadResult(int saved, List<String> problems, String folder) {
    }

    private final LockerFileRepository files;
    private final UserRepository users;
    private final LockerStorage storage;
    private final long maxFileBytes;
    private final ZoneId zone;

    public LockerService(LockerFileRepository files,
                         UserRepository users,
                         LockerStorage storage,
                         @Value("${igraded.locker.max-file-bytes:52428800}") long maxFileBytes,
                         @Value("${igraded.timezone:Asia/Manila}") String timezone) {
        this.files = files;
        this.users = users;
        this.storage = storage;
        this.maxFileBytes = maxFileBytes;
        this.zone = ZoneId.of(timezone);
    }

    public long maxFileBytes() {
        return maxFileBytes;
    }

    /** Usage, folders and the file list in one go. folderParam null or blank means all files. */
    public Overview overview(long ownerId, String folderParam) {
        List<LockerFile> all = files.findByOwnerIdOrderByUploadedAtDesc(ownerId);

        long used = 0;
        Map<String, Integer> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (LockerFile f : all) {
            used += f.getSizeBytes();
            counts.merge(f.getFolder(), 1, Integer::sum);
        }
        List<FolderView> folders = new ArrayList<>();
        counts.forEach((name, count) -> folders.add(new FolderView(name, count)));

        String selected = null;
        if (folderParam != null && !folderParam.isBlank()) {
            String wanted = FileRules.cleanFolder(folderParam);
            selected = counts.keySet().stream()
                    .filter(name -> name.equalsIgnoreCase(wanted))
                    .findFirst()
                    .orElse(wanted);
        }
        final String filter = selected;

        List<FileView> views = all.stream()
                .filter(f -> filter == null || f.getFolder().equalsIgnoreCase(filter))
                .map(this::toView)
                .toList();

        return new Overview(new UsageView(used, quotaOf(ownerId)), folders, views, all.size(), selected);
    }

    /** Saves each chosen file if it passes the checks. Files that fail are reported, the rest still go in. */
    public UploadResult upload(long ownerId, String folderParam, List<MultipartFile> uploads) {
        String wanted = FileRules.cleanFolder(folderParam);
        // "quizzes" and "Quizzes" are the same folder
        String folder = files.findFolders(ownerId).stream()
                .filter(wanted::equalsIgnoreCase)
                .findFirst()
                .orElse(wanted);

        long quota = quotaOf(ownerId);
        long used = usedBytes(ownerId);
        List<String> problems = new ArrayList<>();
        int saved = 0;
        boolean anyChosen = false;

        for (MultipartFile upload : uploads) {
            if (upload.isEmpty()) {
                continue;   // browsers send one empty part when nothing was chosen
            }
            anyChosen = true;

            String name = FileRules.cleanFileName(upload.getOriginalFilename());
            long size = upload.getSize();

            if (!FileRules.isAllowed(name)) {
                problems.add(name + ": this file type is not allowed.");
                continue;
            }
            if (size > maxFileBytes) {
                problems.add(name + ": larger than " + FileRules.humanSize(maxFileBytes) + ".");
                continue;
            }
            if (used + size > quota) {
                problems.add(name + ": not enough space left in your Locker.");
                continue;
            }

            LockerStorage.Stored stored;
            try (InputStream in = upload.getInputStream()) {
                stored = storage.save(ownerId, in);
            } catch (IOException e) {
                log.error("Could not store an upload for user {}", ownerId, e);
                problems.add(name + ": could not be saved, please try again.");
                continue;
            }

            try {
                files.save(new LockerFile(ownerId, folder, name, stored.storedName(),
                        contentTypeOf(upload), stored.size(), LocalDateTime.now(zone)));
                used += stored.size();
                saved++;
            } catch (RuntimeException e) {
                log.error("Could not record an upload for user {}", ownerId, e);
                deleteQuietly(ownerId, stored.storedName());   // no file without a record
                problems.add(name + ": could not be saved, please try again.");
            }
        }

        if (!anyChosen) {
            problems.add("Choose at least one file to upload.");
        }
        return new UploadResult(saved, problems, folder);
    }

    /** The file's record, but only if it belongs to this owner. Otherwise NoSuchElementException. */
    public LockerFile get(long ownerId, long id) {
        return files.findByIdAndOwnerId(id, ownerId).orElseThrow(NoSuchElementException::new);
    }

    public Path pathOf(LockerFile file) {
        return storage.resolve(file.getOwnerId(), file.getStoredName());
    }

    public void delete(long ownerId, long id) {
        LockerFile file = get(ownerId, id);
        files.delete(file);
        deleteQuietly(ownerId, file.getStoredName());
    }

    /** Everything for a ZIP: one folder, or all files when folderParam is blank. */
    public List<ZipExporter.Entry> zipEntries(long ownerId, String folderParam) {
        String wanted = folderParam == null || folderParam.isBlank() ? null : FileRules.cleanFolder(folderParam);
        List<ZipExporter.Entry> entries = new ArrayList<>();
        for (LockerFile f : files.findByOwnerIdOrderByUploadedAtDesc(ownerId)) {
            if (wanted != null && !f.getFolder().equalsIgnoreCase(wanted)) {
                continue;
            }
            entries.add(new ZipExporter.Entry(
                    f.getFolder() + "/" + FileRules.cleanFileName(f.getOriginalName()),
                    pathOf(f)));
        }
        return entries;
    }

    private long usedBytes(long ownerId) {
        Long used = files.usedBytes(ownerId);
        return used == null ? 0 : used;
    }

    private long quotaOf(long ownerId) {
        return users.findById(ownerId)
                .map(AppUser::getStorageQuotaBytes)
                .orElse(AppUser.DEFAULT_QUOTA_BYTES);
    }

    private FileView toView(LockerFile f) {
        return new FileView(f.getId(), f.getOriginalName(), f.getFolder(),
                FileRules.extensionOf(f.getOriginalName()).toUpperCase(Locale.ROOT),
                FileRules.humanSize(f.getSizeBytes()), f.getUploadedAt().format(DATE));
    }

    private static String contentTypeOf(MultipartFile upload) {
        String type = upload.getContentType();
        if (type == null) {
            return null;
        }
        return type.length() > 120 ? type.substring(0, 120) : type;
    }

    private void deleteQuietly(long ownerId, String storedName) {
        try {
            storage.delete(ownerId, storedName);
        } catch (IOException e) {
            log.warn("Could not remove file {} for user {}", storedName, ownerId, e);
        }
    }
}
