package org.doc.document_service.dto;

import java.util.List;

import lombok.Data;

@Data
public class DocumentListResponse {

    private List<DocumentListItem> items;
    private int page;
    private int size;
    private long total;
}
