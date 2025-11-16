package org.doc.document_service.storage;

public interface StorageService {
    
    /**
     * Generate a presigned PUT URL for the given storage key with TTL in seconds.
     */
    PresignedUrlResponse generatePresignedPutUrl(String storageKey, long ttlSeconds);

    /**
     * Check existence of an object at storageKey (used during /complete verification).
     */
    boolean exists(String storageKey);

    /**
     * Generate a presigned GET URL for downloads.
     */
    PresignedUrlResponse generatePresignedGetUrl(String storageKey, long ttlSeconds);

    /**
     * Delete object at storageKey (used by purge jobs).
     */
    void delete(String storageKey);
}
