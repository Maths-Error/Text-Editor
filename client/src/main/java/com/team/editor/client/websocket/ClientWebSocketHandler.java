package com.team.editor.client.websocket;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.protocol.dto.*;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class ClientWebSocketHandler implements OutboundMessageDispatcher {
    private static final int MAX_LOGGED_PAYLOAD_LENGTH = 500;
    private static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:8082/ws/collab";

    public interface Listener {
        void onJoinSessionResponse(JoinSessionResponse response);
        void onError(ErrorMessage errorMessage);
        void onPresenceUpdate(PresenceUpdateMessage message);
        void onCursorUpdate(CursorUpdateMessage message);
        void onOperation(OperationMessage message);
    }

    private final ObjectMapper objectMapper;
    private Listener listener;
    private WebSocket webSocket;
    private String webSocketUrl;

    public ClientWebSocketHandler() {
        this(DEFAULT_WEBSOCKET_URL);
    }

    public ClientWebSocketHandler(String webSocketUrl) {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        setWebSocketUrl(webSocketUrl);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        if (webSocketUrl == null || webSocketUrl.isBlank()) {
            this.webSocketUrl = DEFAULT_WEBSOCKET_URL;
        } else {
            this.webSocketUrl = webSocketUrl.trim();
        }
    }

    public boolean connect() {
        System.out.println("Connecting to WebSocket server: " + webSocketUrl);
        try {
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(webSocketUrl), new WebSocket.Listener() {
                        StringBuilder textBuilder = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            System.out.println("WebSocket Connected.");
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            textBuilder.append(data);
                            if (last) {
                                handleIncoming(textBuilder.toString());
                                textBuilder = new StringBuilder();
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            System.out.println("WebSocket Closed: " + reason);
                            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            System.err.println("WebSocket Error: " + error.getMessage());
                            if (listener != null) {
                                ErrorMessage errorMessage = new ErrorMessage();
                                errorMessage.setMessage("Connection Error: " + error.getMessage());
                                listener.onError(errorMessage);
                            }
                        }
                    }).join();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setMessage("Failed to connect: " + e.getMessage());
                listener.onError(errorMessage);
            }
            return false;
        }
    }

    public void disconnect() {
        if (webSocket == null) {
            return;
        }

        try {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed editor").join();
        } catch (Exception exception) {
            System.err.println("WebSocket close failed: " + exception.getMessage());
        } finally {
            webSocket = null;
        }
    }

    @Override
    public void sendJoinRequest(JoinSessionRequest request) {
        send(request);
    }

    @Override
    public void sendOperation(OperationMessage message) {
        send(message);
    }

    @Override
    public void sendCursorUpdate(CursorUpdateMessage message) {
        send(message);
    }

    @Override
    public void sendUndoRequest(UndoRequestMessage message) {
        send(message);
    }

    @Override
    public void sendRedoRequest(RedoRequestMessage message) {
        send(message);
    }

    private void send(CollaborationMessage message) {
        try {
            System.out.println(
                    "CLIENT WS send invoked -> messageType=" + message.messageType()
                            + ", webSocketPresent=" + (webSocket != null)
            );
            if (webSocket == null) {
                System.err.println(
                        "CLIENT WS send aborted -> messageType=" + message.messageType()
                                + ", reason=webSocket is null"
                );
                return;
            }
            String json = objectMapper.writeValueAsString(message);
            if (message.messageType() == MessageType.OPERATION) {
                System.out.println("CLIENT WS before sendText(OPERATION) -> " + json);
            } else {
                System.out.println("Sending: " + json);
            }
            webSocket.sendText(json, true);
            if (message.messageType() == MessageType.OPERATION) {
                System.out.println("CLIENT WS after sendText(OPERATION) -> queued frame successfully");
            }
        } catch (Exception e) {
            System.err.println("CLIENT WS send failed -> messageType=" + message.messageType());
            e.printStackTrace();
        }
    }

    public void handleIncoming(String json) {
        System.out.println("RECEIVED RAW -> " + json);
        try {
            JsonNode node = objectMapper.readTree(json);
            
            if (!node.has("messageType")) {
                System.out.println("Unknown message format: " + json);
                return;
            }
            
            MessageType type = MessageType.valueOf(node.get("messageType").asText());

            switch (type) {
                case JOIN_SESSION_RESPONSE -> {
                    if (listener != null) {
                        JoinSessionResponse response = objectMapper.treeToValue(node, JoinSessionResponse.class);
                        listener.onJoinSessionResponse(response);
                    }
                }

                case ERROR -> {
                    String code = node.has("errorCode") ? node.get("errorCode").asText() : "UNKNOWN";
                    String msg = node.has("message") ? node.get("message").asText() : "";

                    if (listener != null) {
                        ErrorMessage error = new ErrorMessage();
                        error.setErrorCode(code);
                        error.setMessage(msg);
                        listener.onError(error);
                    }
                }

                case PRESENCE_UPDATE -> {
                    PresenceUpdateMessage msg = objectMapper.treeToValue(node, PresenceUpdateMessage.class);
                    if (listener != null) {
                        listener.onPresenceUpdate(msg);
                    }
                }

                case CURSOR_UPDATE -> {
                    if (listener != null) {
                        CursorUpdateMessage msg = objectMapper.treeToValue(node, CursorUpdateMessage.class);
                        listener.onCursorUpdate(msg);
                    }
                }

                case OPERATION -> {
                    if (listener != null) {
                        JsonNode payloadNode = node.get("payload");
                        System.out.println("RECEIVED OPERATION RAW -> " + abbreviate(json));
                        System.out.println(
                                "RECEIVED OPERATION PAYLOAD -> nodeType="
                                        + describePayloadShape(payloadNode)
                                        + ", payload="
                                        + abbreviate(payloadNode == null ? "null" : payloadNode.toString())
                        );
                        OperationMessage msg = objectMapper.treeToValue(node, OperationMessage.class);
                        String senderId = msg.getSenderId() == null ? "<unknown>" : msg.getSenderId().getValue();
                        System.out.println(
                                "DISPATCHING OPERATION from " + senderId
                                        + " with payloadNodeType=" + describePayloadShape(msg.getPayload())
                                        + " and payload=" + abbreviate(msg.getPayload() == null ? "null" : msg.getPayload().toString())
                        );
                        listener.onOperation(msg);
                    }
                }

                default -> System.out.println("Unsupported incoming message type: " + type);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String describePayloadShape(JsonNode payloadNode) {
        return payloadNode == null ? "missing" : payloadNode.getNodeType().name();
    }

    private String abbreviate(String payload) {
        if (payload == null || payload.length() <= MAX_LOGGED_PAYLOAD_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_LOGGED_PAYLOAD_LENGTH) + "...";
    }
}
