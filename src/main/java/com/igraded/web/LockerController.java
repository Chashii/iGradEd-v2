package com.igraded.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.locker.FileRules;
import com.igraded.locker.LockerFile;
import com.igraded.locker.LockerService;
import com.igraded.locker.ZipExporter;
import com.igraded.security.AppUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The Locker pages. Every action uses the logged-in teacher's own id, never an id from the request,
 * so nobody can reach another teacher's files.
 */
@Controller
public class LockerController {

    private final LockerService locker;

    public LockerController(LockerService locker) {
        this.locker = locker;
    }

    @GetMapping("/locker")
    public String page(@AuthenticationPrincipal AppUserDetails me,
                       @RequestParam(required = false) String folder,
                       Model model) {
        LockerService.Overview overview = locker.overview(me.getId(), folder);

        model.addAttribute("active", "locker");
        model.addAttribute("usage", overview.usage());
        model.addAttribute("folders", overview.folders());
        model.addAttribute("files", overview.files());
        model.addAttribute("totalCount", overview.totalCount());
        model.addAttribute("selectedFolder", overview.selectedFolder());
        model.addAttribute("maxFileBytes", locker.maxFileBytes());
        model.addAttribute("maxFileLabel", FileRules.humanSize(locker.maxFileBytes()));
        model.addAttribute("accept", FileRules.allowedExtensions().stream()
                .map(ext -> "." + ext)
                .collect(Collectors.joining(",")));
        model.addAttribute("typesLabel", FileRules.allowedExtensions().stream()
                .map(String::toUpperCase)
                .collect(Collectors.joining(", ")));
        return "locker";
    }

    @PostMapping("/locker/upload")
    public String upload(@AuthenticationPrincipal AppUserDetails me,
                         @RequestParam(required = false) String folder,
                         @RequestParam(name = "files", required = false) List<MultipartFile> files,
                         RedirectAttributes redirect) {
        LockerService.UploadResult result =
                locker.upload(me.getId(), folder, files == null ? List.of() : files);

        redirect.addFlashAttribute("saved", result.saved());
        redirect.addFlashAttribute("problems", result.problems());
        redirect.addAttribute("folder", result.folder());
        return "redirect:/locker";
    }

    @GetMapping("/locker/files/{id}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AppUserDetails me,
                                             @PathVariable Long id) throws IOException {
        LockerFile file = find(me, id);
        Path path = locker.pathOf(file);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(FileRules.cleanFileName(file.getOriginalName()), StandardCharsets.UTF_8)
                .build();

        // Always sent as a download with a neutral type, so the browser never runs or displays an upload
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(path))
                .body(new FileSystemResource(path));
    }

    @PostMapping("/locker/files/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUserDetails me,
                         @PathVariable Long id,
                         @RequestParam(required = false) String folder,
                         RedirectAttributes redirect) {
        try {
            locker.delete(me.getId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirect.addFlashAttribute("deleted", true);
        if (folder != null && !folder.isBlank()) {
            redirect.addAttribute("folder", folder);
        }
        return "redirect:/locker";
    }

    /** Downloads a ZIP of one folder, or of everything when no folder is given. */
    @GetMapping("/locker/export")
    public void export(@AuthenticationPrincipal AppUserDetails me,
                       @RequestParam(required = false) String folder,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
        List<ZipExporter.Entry> entries = locker.zipEntries(me.getId(), folder);
        if (entries.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/locker?empty");
            return;
        }

        String base = folder == null || folder.isBlank() ? "iGradEd-locker" : "iGradEd-" + FileRules.cleanFolder(folder);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(base + "-" + LocalDate.now() + ".zip", StandardCharsets.UTF_8)
                .build();

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        ZipExporter.write(response.getOutputStream(), entries);
    }

    private LockerFile find(AppUserDetails me, Long id) {
        try {
            return locker.get(me.getId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
