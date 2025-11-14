package org.doc.document_service.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import jakarta.validation.constraints.Pattern;


@Data
public class DocumentCompleteRequest {

    /**
     * Final size in bytes (optional)
     */
    @PositiveOrZero(message = "size must be >= 0")
    private Long size;

    /**
     * Optional SHA-256 hex checksum from client (lowercase hex)
     */
    @Pattern(regexp = "^[a-f0-9]{64}$", message = "checksum must be lowercase sha256 hex (64 chars)")
    private String checksum;

    /**
     * uploadMethod: presigned | proxy (optional, for analytics)
     */
    @Pattern(regexp = "^(presigned|proxy)?$", message = "invalid uploadMethod")
    private String uploadMethod;
}
