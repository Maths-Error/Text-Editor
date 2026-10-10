package com.team.editor.client.service;

public class ClientApiException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public ClientApiException(int statusCode, String responseBody) {
        super("API request failed with status " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
