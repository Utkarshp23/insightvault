package org.srh.search_service.controller;

import org.srh.search_service.model.DocumentIndex;
import org.srh.search_service.repo.DocumentSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private DocumentSearchRepository repository;

    @PreAuthorize("hasAuthority('SCOPE_doc:read')")
    @GetMapping
    public ResponseEntity<Page<DocumentIndex>> search(
            @RequestParam String query,
            Authentication authentication, // In real app, extract from JWT
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Securely extract owner ID from the Token
        String ownerId = extractOwnerId(authentication);

        System.out.println(">>> SEARCH DEBUG: Query='" + query + "', Owner='" + ownerId + "'");

        Page<DocumentIndex> results = repository.findByContentContainingAndOwnerId(query, ownerId,
                PageRequest.of(page, size));

        System.out.println(">>> SEARCH DEBUG: Found " + results.getTotalElements() + " hits");

        return ResponseEntity.ok(results);
    }

    private String extractOwnerId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("No authentication present");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject(); // The "sub" claim from the JWT
        }
        // Fallback for non-JWT auth (if any)
        return authentication.getName();
    }
}
