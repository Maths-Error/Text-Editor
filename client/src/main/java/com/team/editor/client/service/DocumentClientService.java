package com.team.editor.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.client.config.ClientConfig;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DeleteDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentSummaryDto;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.RenameDocumentRequest;
import com.team.editor.common.protocol.dto.document.RenameDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DocumentClientService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String serverBaseUrl;

    public DocumentClientService() {
        this(new ClientConfig());
    }

    public DocumentClientService(ClientConfig config) {
        this(config == null ? null : config.getServerBaseUrl());
    }

    public DocumentClientService(String serverBaseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.serverBaseUrl = ClientConfig.normalizeBaseUrl(serverBaseUrl);
    }

    public CreateDocumentResponse createDocument(CreateDocumentRequest request) {
        return post("/api/documents", request, CreateDocumentResponse.class);
    }

    public List<DocumentSummaryDto> listDocuments(String userId) {
        String path = "/api/documents?userId=" + encodeQueryValue(userId);
        ListDocumentsResponse response = get(path, ListDocumentsResponse.class);
        return response.getDocuments();
    }

    public OpenDocumentResponse openDocumentById(String documentId, String userId) {
        String path = "/api/documents/" + encodePathValue(documentId)
                + "?userId=" + encodeQueryValue(userId);
        return get(path, OpenDocumentResponse.class);
    }

    public OpenDocumentResponse openDocumentByCode(OpenDocumentRequest request) {
        return post("/api/documents/open-by-code", request, OpenDocumentResponse.class);
    }

    public ShareCodeResponse getShareCodes(String documentId, String requesterUserId) {
        String path = "/api/documents/" + encodePathValue(documentId)
                + "/share-codes?requesterUserId=" + encodeQueryValue(requesterUserId);
        return get(path, ShareCodeResponse.class);
    }

    public RenameDocumentResponse renameDocument(String documentId, RenameDocumentRequest request) {
        String path = "/api/documents/" + encodePathValue(documentId);
        return patch(path, request, RenameDocumentResponse.class);
    }

    public DeleteDocumentResponse deleteDocument(String documentId, String requesterUserId) {
        String path = "/api/documents/" + encodePathValue(documentId)
                + "?requesterUserId=" + encodeQueryValue(requesterUserId);
        return delete(path, DeleteDocumentResponse.class);
    }

    public SaveSnapshotResponse saveSnapshot(String documentId, SaveSnapshotRequest request) {
        String path = "/api/documents/" + encodePathValue(documentId) + "/snapshot";
        return put(path, request, SaveSnapshotResponse.class);
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = ClientConfig.normalizeBaseUrl(serverBaseUrl);
    }

    private <T> T get(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .GET()
                .build();
        return send(request, responseType);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return sendWithBody("POST", path, body, responseType);
    }

    private <T> T put(String path, Object body, Class<T> responseType) {
        return sendWithBody("PUT", path, body, responseType);
    }

    private <T> T patch(String path, Object body, Class<T> responseType) {
        return sendWithBody("PATCH", path, body, responseType);
    }

    private <T> T delete(String path, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildUri(path))
                .DELETE()
                .build();
        return send(request, responseType);
    }

    private <T> T sendWithBody(String method, String path, Object body, Class<T> responseType) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(buildUri(path))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return send(request, responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write request JSON.", exception);
        }
    }

    private <T> T send(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode < 200 || statusCode >= 300) {
                throw new ClientApiException(statusCode, responseBody);
            }

            return objectMapper.readValue(responseBody, responseType);
        } catch (ClientApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read API response.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("API request was interrupted.", exception);
        }
    }

    private URI buildUri(String path) {
        return URI.create(serverBaseUrl + path);
    }

    private String encodePathValue(String value) {
        return encodeQueryValue(value).replace("+", "%20");
    }

    private String encodeQueryValue(String value) {
        String safeValue = value == null ? "" : value;
        return URLEncoder.encode(safeValue, StandardCharsets.UTF_8);
    }
}
