package com.coderpage.mine.app.tally.sync;

import java.io.IOException;

/**
 * Notion 网络网关，封装 API 访问
 */
public class NotionGateway {

    private final NotionApiClient apiClient;

    public NotionGateway(NotionApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public boolean validateConfig() {
        return apiClient.validateConfig();
    }

    public String queryDatabase(String cursor) throws IOException {
        return apiClient.queryDatabase(cursor);
    }

    public String createPage(String pageData) throws IOException {
        return apiClient.createPage(pageData);
    }

    public String updatePage(String pageId, String pageData) throws IOException {
        return apiClient.updatePage(pageId, pageData);
    }

    public String getDatabaseSchema() throws IOException {
        return apiClient.getDatabaseSchema();
    }
}
