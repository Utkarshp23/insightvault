package org.doc.document_service.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class StorageKeyUtil {

    private StorageKeyUtil() {}

    /**
     * Example: documents/{tenantId or "global"}/{documentId}/{url-encoded-filename}
     */
    public static String generateStorageKey(String tenantId, UUID documentId, String filename) {
        String tenant = (tenantId == null || tenantId.isBlank()) ? "global" : sanitize(tenantId);
        String name = filename == null ? documentId.toString() : sanitize(filename);
        return String.format("documents/%s/%s/%s", tenant, documentId.toString(), name);
    }

    private static String sanitize(String s) {
        // Basic URL-encode; you can add more rules (remove path separators, length limits)
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}