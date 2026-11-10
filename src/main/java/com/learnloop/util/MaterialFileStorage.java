package com.learnloop.util;

import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded class material files outside the web application root and only exposes them
 * through an authorized download servlet.
 */
public final class MaterialFileStorage {

    public static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    public static final int MAX_FILE_NAME_LENGTH = 255;
    public static final String DIRECTORY_PROPERTY = "learnloop.materials.dir";
    public static final String DIRECTORY_ENV = "LEARNLOOP_MATERIALS_DIR";

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "csv",
            "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "ods", "odp",
            "png", "jpg", "jpeg", "gif", "webp", "zip");

    private final Path root;

    public MaterialFileStorage() {
        this(defaultDirectory());
    }

    public MaterialFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public static Path defaultDirectory() {
        String configured = System.getProperty(DIRECTORY_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(DIRECTORY_ENV);
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured.trim());
        }
        return Paths.get(System.getProperty("user.home"), "learnloop-materials");
    }

    public Path root() {
        return root;
    }

    public static boolean isAllowedExtension(String fileName) {
        String extension = extensionOf(fileName);
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    public static String allowedExtensionsLabel() {
        return "PDF, TXT, MD, CSV, DOC/DOCX, PPT/PPTX, XLS/XLSX, ODT/ODS/ODP, "
                + "PNG, JPG/JPEG, GIF, WEBP or ZIP";
    }

    public StoredFile store(Part part) throws IOException {
        if (part == null || part.getSize() <= 0) {
            throw new IllegalArgumentException("Choose a file to upload.");
        }
        if (part.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Files must be 10 MB or smaller.");
        }
        String originalName = sanitizeFileName(part.getSubmittedFileName());
        if (originalName == null) {
            throw new IllegalArgumentException("The uploaded file needs a name.");
        }
        if (!isAllowedExtension(originalName)) {
            throw new IllegalArgumentException(
                    "This file type is not allowed. Accepted types: " + allowedExtensionsLabel() + ".");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + "." + extensionOf(originalName);
        Files.createDirectories(root);
        Path destination = resolve(storedName);
        try (InputStream input = part.getInputStream()) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        String mimeType = mimeTypeForExtension(originalName);
        return new StoredFile(storedName, originalName, mimeType, part.getSize());
    }

    public Path resolve(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new IllegalArgumentException("Missing stored file name.");
        }
        Path resolved = root.resolve(storedName).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid stored file name.");
        }
        return resolved;
    }

    public boolean exists(String storedName) {
        return storedName != null && !storedName.isBlank() && Files.isRegularFile(resolve(storedName));
    }

    public void deleteQuietly(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | IllegalArgumentException ignored) {
            // A missing or invalid file must not break the material record deletion.
        }
    }

    static String sanitizeFileName(String submittedName) {
        if (submittedName == null) {
            return null;
        }
        String candidate = submittedName.trim();
        int slash = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        if (slash >= 0) {
            candidate = candidate.substring(slash + 1);
        }
        StringBuilder sanitized = new StringBuilder(candidate.length());
        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            if (Character.isISOControl(character) || character == ':' || character == '*'
                    || character == '?' || character == '"' || character == '<'
                    || character == '>' || character == '|') {
                sanitized.append('_');
            } else {
                sanitized.append(character);
            }
        }
        String cleaned = sanitized.toString().trim();
        while (cleaned.endsWith(".")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.length() > MAX_FILE_NAME_LENGTH) {
            String extension = extensionOf(cleaned);
            int suffixLength = extension == null ? 0 : extension.length() + 1;
            int baseLength = Math.max(1, MAX_FILE_NAME_LENGTH - suffixLength);
            String base = cleaned.substring(0, baseLength);
            cleaned = extension == null ? base : base + "." + extension;
        }
        return cleaned;
    }

    static String extensionOf(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Never trust the browser-supplied Content-Type header. Serve uploads with a type
     * derived from the extension allowlist; downloads are also forced to attachment.
     */
    public static String mimeTypeForExtension(String fileName) {
        String extension = extensionOf(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain; charset=UTF-8";
            case "md" -> "text/markdown; charset=UTF-8";
            case "csv" -> "text/csv; charset=UTF-8";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            case "odp" -> "application/vnd.oasis.opendocument.presentation";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    public record StoredFile(String storedName, String originalName, String mimeType, long sizeBytes) {
    }
}
