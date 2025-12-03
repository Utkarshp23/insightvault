package org.srh.search_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.UUID;

@Data
@Document(indexName = "documents")
public class DocumentIndex {

    @Id
    private UUID id;

    @Field(type = FieldType.Keyword)
    private String ownerId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String filename;

    @Field(type = FieldType.Text, analyzer = "english")
    private String content; // The full extracted text

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private List<String> keywords;

    @Field(type = FieldType.Keyword)
    private String category;
}