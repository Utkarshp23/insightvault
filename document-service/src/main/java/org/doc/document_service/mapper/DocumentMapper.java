package org.doc.document_service.mapper;
import org.doc.document_service.domain.Document;
import org.doc.document_service.domain.DocumentStatus;
import org.doc.document_service.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import lombok.Data;

import java.util.UUID;

@Mapper(componentModel = "spring", uses = { JsonMapper.class })
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    @Mapping(target = "id", source = "documentId")
    @Mapping(target = "ownerId", source = "ownerId")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "filename", source = "filename")
    @Mapping(target = "mimeType", source = "mimeType")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "storageKey", source = "storageKey")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "checksum", source = "checksum")
    @Mapping(target = "visibility", source = "visibility")
    @Mapping(target = "metadata", expression = "java(jsonMapper.toJson(dto.getMetadata()))")
    @Mapping(target = "requestId", source = "requestId")
    Document toEntity(DocumentCreateRequest dto, @Context JsonMapper jsonMapper);

    // Map entity -> metadata response
    @Mapping(source = "id", target = "documentId")
    @Mapping(source = "ownerId", target = "ownerId")
    @Mapping(source = "tenantId", target = "tenantId")
    @Mapping(source = "filename", target = "filename")
    @Mapping(source = "mimeType", target = "mimeType")
    @Mapping(source = "size", target = "size")
    @Mapping(source = "storageKey", target = "storageKey")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "checksum", target = "checksum")
    @Mapping(source = "visibility", target = "visibility")
    @Mapping(target = "metadata", expression = "java(jsonMapper.fromJson(entity.getMetadata()))")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(source = "requestId", target = "requestId")
    DocumentMetadataResponse toMetadataResponse(Document entity, @Context JsonMapper jsonMapper);

    // For create-response keep only a few fields
    @Mapping(source = "id", target = "documentId")
    @Mapping(source = "storageKey", target = "storageKey")
    @Mapping(target = "presignedUrl", ignore = true) // set by service after mapper call
    @Mapping(target = "presignedUrlExpiresAt", ignore = true)
    @Mapping(target = "ttlSeconds", ignore = true)
    @Mapping(source = "status", target = "status")
    DocumentCreateResponse toCreateResponse(Document entity);

    @Mapping(source = "id", target = "documentId")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "processingProgress", ignore = true)
    @Mapping(target = "error", ignore = true)
    @Mapping(source = "updatedAt", target = "lastUpdated")
    DocumentStatusResponse toStatusResponse(Document entity);

    @AfterMapping
    default void ensureId(DocumentCreateResponse resp, @MappingTarget DocumentCreateResponse target) {
        // no-op placeholder if you want to adjust responses post map
    }
}
