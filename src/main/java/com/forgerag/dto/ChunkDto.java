package com.forgerag.dto;

/**
 * DTO for a single document chunk returned by GET /api/chunks.
 * Field names match the frozen API contract camelCase shape.
 */
public class ChunkDto {

    private long id;
    private String content;
    private String sourceDoc;
    private String createdAt;

    public ChunkDto() {}

    public ChunkDto(long id, String content, String sourceDoc, String createdAt) {
        this.id = id;
        this.content = content;
        this.sourceDoc = sourceDoc;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSourceDoc() { return sourceDoc; }
    public void setSourceDoc(String sourceDoc) { this.sourceDoc = sourceDoc; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
