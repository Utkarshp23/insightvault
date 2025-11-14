package org.doc.document_service.domain;

public enum DocumentStatus {
    UPLOADING,
    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED,
    QUARANTINE,
    DELETED
}
