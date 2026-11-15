package com.konate.ithelpdesk.triage.BusinessLayer;

import com.konate.ithelpdesk.config.CodedApiException;
import com.konate.ithelpdesk.triage.AiLayer.ValidatedImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates uploads from their actual bytes (magic numbers), not the filename or Content-Type header.
 * Accepts PNG, JPEG and WebP only.
 */
@Component
public class ImageValidator {

    private final int maxFiles;
    private final long maxFileBytes;
    private final long maxTotalBytes;

    public ImageValidator(@Value("${tiqnest.triage.max-files:4}") int maxFiles,
                          @Value("${tiqnest.triage.max-file-bytes:3670016}") long maxFileBytes,
                          @Value("${tiqnest.triage.max-total-bytes:10485760}") long maxTotalBytes) {
        this.maxFiles = maxFiles;
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    public List<ValidatedImage> validate(List<MultipartFile> files) {
        List<MultipartFile> present = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (present.isEmpty()) {
            throw new CodedApiException(HttpStatus.BAD_REQUEST, "BLANK_UPLOAD",
                    "Please attach at least one screenshot (PNG, JPEG or WebP).");
        }
        if (present.size() > maxFiles) {
            throw new CodedApiException(HttpStatus.BAD_REQUEST, "TOO_MANY_FILES",
                    "Please attach at most " + maxFiles + " images.");
        }
        long total = 0;
        List<ValidatedImage> images = new ArrayList<>();
        for (MultipartFile file : present) {
            if (file.getSize() > maxFileBytes) {
                throw new CodedApiException(HttpStatus.CONTENT_TOO_LARGE, "IMAGE_TOO_LARGE",
                        "Each image must be under " + (maxFileBytes / (1024 * 1024.0)) + " MB.");
            }
            total += file.getSize();
            if (total > maxTotalBytes) {
                throw new CodedApiException(HttpStatus.CONTENT_TOO_LARGE, "UPLOAD_TOO_LARGE",
                        "The images together must be under " + (maxTotalBytes / (1024 * 1024)) + " MB.");
            }
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new CodedApiException(HttpStatus.BAD_REQUEST, "UNREADABLE_UPLOAD", "The upload could not be read.");
            }
            String mediaType = detectMediaType(bytes);
            if (mediaType == null) {
                throw new CodedApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE",
                        "Only PNG, JPEG and WebP screenshots are supported.");
            }
            images.add(new ValidatedImage(mediaType, bytes));
        }
        return images;
    }

    /** Returns the real media type from the file signature, or null if it is not a supported image. */
    public static String detectMediaType(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "image/png";
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }
}
