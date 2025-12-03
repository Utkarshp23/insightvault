package org.srh.search_service.repo;

import org.srh.search_service.model.DocumentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentSearchRepository extends ElasticsearchRepository<DocumentIndex, UUID> {
    
    // Basic search: Find by content matching text AND owner
    Page<DocumentIndex> findByContentContainingAndOwnerId(String content, String ownerId, Pageable pageable);
    
    // Find by filename OR content
    Page<DocumentIndex> findByOwnerIdAndFilenameContainingOrContentContaining(String ownerId, String filename, String content, Pageable pageable);
}
