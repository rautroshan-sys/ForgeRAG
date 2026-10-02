package com.forgerag.dto;

import jakarta.validation.constraints.NotBlank;

public class QueryRequest {

    @NotBlank(message = "query must not be blank")
    private String query;

    public QueryRequest() {}

    public QueryRequest(String query) { this.query = query; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
