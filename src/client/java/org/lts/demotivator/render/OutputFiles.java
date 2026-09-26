package org.lts.demotivator.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class OutputFiles {
    public static final String DEFAULT_FOLDER = "demotivators";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private OutputFiles() {
    }

    public static String sanitizeFolder(String folder) {
        if (folder == null) {
            return DEFAULT_FOLDER;
        }
        String cleaned = folder.strip()
                .replaceAll("[^A-Za-z0-9 _.-]", "_")
                .replaceAll("^[. ]+|[. ]+$", "");
        if (cleaned.isEmpty() || cleaned.length() > 64 || isReservedName(cleaned)) {
            return DEFAULT_FOLDER;
        }
        return cleaned;
    }

    private static boolean isReservedName(String name) {
        int dot = name.indexOf('.');
        String base = (dot < 0 ? name : name.substring(0, dot)).strip().toUpperCase(Locale.ROOT);
        return base.matches("CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9]");
    }

    public static Path uniqueFile(Path directory, LocalDateTime time) {
        String base = "demotivator_" + STAMP.format(time);
        Path candidate = directory.resolve(base + ".png");
        for (int index = 1; Files.exists(candidate); index++) {
            candidate = directory.resolve(base + "_" + index + ".png");
        }
        return candidate;
    }
}
