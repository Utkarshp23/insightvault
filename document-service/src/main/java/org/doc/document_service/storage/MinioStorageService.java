package org.doc.document_service.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService implements StorageService {


    @Autowired
    private MinioClient client;

    @Autowired
    private StorageProperties props;

    @Override
    public PresignedUrlResponse generatePresignedPutUrl(String storageKey, long ttlSeconds) {
        try {
            String url = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .bucket(props.getBucket())
                            .object(storageKey)
                            .method(Method.PUT)
                            .expiry((int) ttlSeconds, TimeUnit.SECONDS)
                            .build()
            );

            return new PresignedUrlResponse(url, Instant.now().plusSeconds(ttlSeconds), ttlSeconds);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate presigned PUT URL", ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(storageKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("Error checking object existence", e);
        } catch (Exception ex) {
            throw new RuntimeException("Error checking object existence", ex);
        }
    }

    @Override
    public PresignedUrlResponse generatePresignedGetUrl(String storageKey, long ttlSeconds) {
        try {
            String url = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs
                            .builder()
                            .bucket(props.getBucket())
                            .object(storageKey)
                            .method(Method.GET)
                            .expiry((int) ttlSeconds, TimeUnit.SECONDS)
                            .build()
            );

            return new PresignedUrlResponse(url, Instant.now().plusSeconds(ttlSeconds), ttlSeconds);

        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate presigned GET URL", ex);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(storageKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete object", ex);
        }
    }
}
