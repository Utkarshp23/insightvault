package org.doc.document_service.storage;

import java.time.Instant;

public class PresignedUrlResponse {
    private final String url;
    private final Instant expiresAt;
    private final long ttlSeconds;

    public PresignedUrlResponse(String url, Instant expiresAt, long ttlSeconds) {
        this.url = url;
        this.expiresAt = expiresAt;
        this.ttlSeconds = ttlSeconds;
    }

    public String getUrl() { return url; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getTtlSeconds() { return ttlSeconds; }
}
