package com.igraded.locker;

/** One row in the file list, with everything already formatted for the page. */
public record FileView(Long id, String name, String folder, String ext, String size, String uploaded) {
}
