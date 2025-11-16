package org.doc.document_service.mapper;
import org.doc.document_service.domain.Document;
import org.doc.document_service.dto.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring", uses = { JsonMapper.class })
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    @Mapping(target = "id", expression = "java(generateUuid())")
    @Mapping(target = "storageKey", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    // fields that are not provided by create request should be left null or set elsewhere:
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Document toDocument(DocumentCreateRequest request);

    // helper used by MapStruct-generated code — provide here so generated impl can call it
    default java.util.UUID generateUuid() {
        return java.util.UUID.randomUUID();
    }

    @AfterMapping
    default void enrichDocument(DocumentCreateRequest request, @MappingTarget Document target) {
        // ensure id exists (in case MapStruct didn't call generateUuid for some reason)
        if (target.getId() == null) {
            target.setId(generateUuid());
        }
        // Build storage key using util (tenantId not available at create request level)
        target.setStorageKey(org.doc.document_service.util.StorageKeyUtil.generateStorageKey(null, target.getId(), request.getFilename()));
        // Serialize metadata map to JSON using the JsonMapper component (safe to instantiate here)
        target.setMetadata(new JsonMapper().toJson(request.getMetadata()));
    }

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
