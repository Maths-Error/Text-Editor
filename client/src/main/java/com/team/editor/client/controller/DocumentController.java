package com.team.editor.client.controller;

import com.team.editor.client.config.ClientConfig;
import com.team.editor.client.file.TextImportService;
import com.team.editor.client.service.ClientApiException;
import com.team.editor.client.service.DocumentClientService;
import com.team.editor.client.service.DocumentSessionService;
import com.team.editor.client.state.ConnectionState;
import com.team.editor.client.state.EditorState;
import com.team.editor.client.ui.DocumentDashboardView;
import com.team.editor.client.ui.EditorView;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.DocumentSummaryDto;
import com.team.editor.common.protocol.dto.document.DeleteDocumentResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class DocumentController implements SessionController.SessionListener {
    private final ClientConfig config;
    private final DocumentClientService documentClientService;
    private final DocumentSessionService documentSessionService;
    private final SessionController sessionController;
    private final TextImportService textImportService;
    private DocumentDashboardView view;
    private EditorController editorController;
    private EditorView editorView;
    private String joiningUserId;
    private String joiningDisplayName;
    private boolean joining;
    private boolean editorOpen;
    private String liveDocumentId;
    private String statusAfterEditorOpen;

    public DocumentController() {
        this(new ClientConfig(), null, null, null);
    }

    public DocumentController(
            ClientConfig config,
            DocumentClientService documentClientService,
            DocumentSessionService documentSessionService
    ) {
        this(config, documentClientService, documentSessionService, null);
    }

    public DocumentController(
            ClientConfig config,
            DocumentClientService documentClientService,
            DocumentSessionService documentSessionService,
            SessionController sessionController
    ) {
        this.config = config == null ? new ClientConfig() : config;
        this.documentClientService = documentClientService == null
                ? new DocumentClientService(this.config)
                : documentClientService;
        this.documentSessionService = documentSessionService == null
                ? new DocumentSessionService()
                : documentSessionService;
        this.sessionController = sessionController == null
                ? new SessionController(this.config)
                : sessionController;
        this.textImportService = new TextImportService();
        this.documentClientService.setServerBaseUrl(this.config.getServerBaseUrl());
        this.sessionController.addListener(this);
    }

    public void setView(DocumentDashboardView view) {
        this.view = view;
    }

    public String getServerBaseUrl() {
        return config.getServerBaseUrl();
    }

    public DocumentSessionService getDocumentSessionService() {
        return documentSessionService;
    }

    public void createDocument(String serverUrl, String userId, String title) {
        try {
            updateServerUrl(serverUrl);
            String safeUserId = requireText(userId, "User ID");
            String safeTitle = title == null || title.isBlank() ? "Untitled Document" : title.trim();

            CreateDocumentRequest request = new CreateDocumentRequest(new UserId(safeUserId), safeTitle, null);
            documentSessionService.loadFromCreateResponse(documentClientService.createDocument(request));

            updateViewAfterOpen("Document created successfully.");
        } catch (Exception exception) {
            showError("Create document failed", exception);
        }
    }

    public void createDocumentAndOpenEditor(String serverUrl, String displayName, String title) {
        try {
            updateServerUrl(serverUrl);
            String safeDisplayName = requireText(displayName, "Display name");
            String safeTitle = requireText(title, "Document title");

            if (view != null) {
                view.showStatus("Creating document...");
            }

            // Run HTTP + WebSocket work on a background thread to avoid freezing the UI
            new Thread(() -> {
                try {
                    CreateDocumentRequest request = new CreateDocumentRequest(new UserId(safeDisplayName), safeTitle, null);
                    documentSessionService.loadFromCreateResponse(documentClientService.createDocument(request));

                    SwingUtilities.invokeLater(() -> {
                        if (view != null) {
                            view.showCurrentDocument(documentSessionService);
                            view.showStatus("Document created. Opening editor...");
                        }
                    });

                    openCurrentDocumentInEditor(serverUrl, safeDisplayName, safeDisplayName);
                } catch (Exception exception) {
                    SwingUtilities.invokeLater(() -> showError("Create document failed", exception));
                }
            }, "create-doc-thread").start();
        } catch (Exception exception) {
            showError("Create document failed", exception);
        }
    }

    public void importFileAndOpenEditor(String serverUrl, String displayName) {
        try {
            updateServerUrl(serverUrl);
            String safeDisplayName = requireText(displayName, "Display name");

            if (editorOpen) {
                throw new IllegalArgumentException("An editor is already open. Please close it before importing another document.");
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Import Text File");
            chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(view);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File selectedFile = chooser.getSelectedFile();
            if (selectedFile == null) {
                return;
            }

            if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
                showErrorMessage("Import failed", "Only .txt files are supported.");
                return;
            }

            if (view != null) {
                view.showStatus("Importing file...");
            }

            new Thread(() -> importFileInBackground(serverUrl, safeDisplayName, selectedFile), "import-file-thread").start();
        } catch (Exception exception) {
            showError("Import failed", exception);
        }
    }

    public void listDocuments(String serverUrl, String userId) {
        try {
            updateServerUrl(serverUrl);
            String safeUserId = requireText(userId, "Display name");

            List<DocumentSummaryDto> documents = documentClientService.listDocuments(safeUserId);
            if (view != null) {
                view.showDocumentList(documents);
            }
        } catch (Exception exception) {
            showError("List documents failed", exception);
        }
    }

    public void deleteDocumentFromList(String serverUrl, String documentId, String displayName, String title) {
        try {
            updateServerUrl(serverUrl);
            String safeDocumentId = requireText(documentId, "selected document");
            String safeDisplayName = requireText(displayName, "Display name");
            String safeTitle = title == null || title.isBlank() ? "Untitled Document" : title.trim();

            if (isDocumentOpenInEditor(safeDocumentId)) {
                if (view != null) {
                    view.showWarning("Close Editor First", "Close the editor window before deleting this document.");
                }
                return;
            }

            if (view != null) {
                view.showStatus("Deleting document...");
            }

            new Thread(
                    () -> deleteDocumentInBackground(safeDocumentId, safeDisplayName, safeTitle),
                    "delete-doc-thread"
            ).start();
        } catch (Exception exception) {
            showError("Delete document failed", exception);
        }
    }

    public boolean isDocumentOpenInEditor(String documentId) {
        return editorOpen && documentId != null && documentId.equals(liveDocumentId);
    }

    public void openDocumentFromList(String serverUrl, String documentId, String displayName) {
        try {
            updateServerUrl(serverUrl);
            String safeDocumentId = requireText(documentId, "selected document");
            String safeDisplayName = requireText(displayName, "Display name");

            if (view != null) {
                view.showStatus("Opening document...");
            }

            // Run HTTP + WebSocket work on a background thread to avoid freezing the UI
            new Thread(() -> {
                try {
                    OpenDocumentResponse response = documentClientService.openDocumentById(safeDocumentId, safeDisplayName);
                    documentSessionService.loadFromOpenResponse(response);

                    SwingUtilities.invokeLater(() -> {
                        if (view != null) {
                            view.showCurrentDocument(documentSessionService);
                            view.showStatus("Document opened. Joining live session...");
                        }
                    });

                    openCurrentDocumentInEditor(serverUrl, safeDisplayName, safeDisplayName);
                } catch (Exception exception) {
                    SwingUtilities.invokeLater(() -> showError("Open selected document failed", exception));
                }
            }, "open-doc-thread").start();
        } catch (Exception exception) {
            showError("Open selected document failed", exception);
        }
    }

    public void openDocumentById(String serverUrl, String documentId, String userId) {
        try {
            updateServerUrl(serverUrl);
            String safeDocumentId = requireText(documentId, "Document ID");
            String safeUserId = requireText(userId, "User ID");

            OpenDocumentResponse response = documentClientService.openDocumentById(safeDocumentId, safeUserId);
            documentSessionService.loadFromOpenResponse(response);

            updateViewAfterOpen("Document opened successfully.");
        } catch (Exception exception) {
            showError("Open document failed", exception);
        }
    }

    private void importFileInBackground(String serverUrl, String displayName, File selectedFile) {
        String documentTitle = buildDocumentTitleFromFile(selectedFile);
        try {
            String importedText = textImportService.importText(selectedFile);
            importedText = importedText.replace("\r\n", "\n").replace("\r", "\n");

            CreateDocumentRequest request = new CreateDocumentRequest(new UserId(displayName), documentTitle, null);
            documentSessionService.loadFromCreateResponse(documentClientService.createDocument(request));

            EditorState importedState = new EditorState(new UserId(displayName), documentSessionService.getDocumentId());
            CrdtSnapshotDto importedSnapshot = importedState.toSnapshotDto(importedText);
            SaveSnapshotRequest snapshotRequest = new SaveSnapshotRequest(
                    new UserId(displayName),
                    documentSessionService.getSnapshotVersion(),
                    importedSnapshot
            );
            SaveSnapshotResponse snapshotResponse = documentClientService.saveSnapshot(
                    documentSessionService.getDocumentId(),
                    snapshotRequest
            );
            documentSessionService.updateSnapshot(importedSnapshot, snapshotResponse.getSnapshotVersion());
            statusAfterEditorOpen = "Imported file and created document: " + documentTitle;

            SwingUtilities.invokeLater(() -> {
                if (view != null) {
                    view.showCurrentDocument(documentSessionService);
                    view.showStatus(statusAfterEditorOpen + ". Opening editor...");
                }
            });

            openCurrentDocumentInEditor(serverUrl, displayName, displayName);
        } catch (IOException exception) {
            SwingUtilities.invokeLater(() -> showErrorMessage(
                    "Import failed",
                    "Could not import file: " + exception.getMessage()
            ));
        } catch (Exception exception) {
            SwingUtilities.invokeLater(() -> showErrorMessage(
                    "Import failed",
                    "Could not create document from imported file: " + describeException(exception)
            ));
        }
    }

    private void deleteDocumentInBackground(String documentId, String displayName, String title) {
        try {
            DeleteDocumentResponse response = documentClientService.deleteDocument(documentId, displayName);
            if (response == null || !response.isDeleted()) {
                throw new IllegalStateException("The server did not confirm that the document was deleted.");
            }

            List<DocumentSummaryDto> refreshedDocuments = null;
            try {
                refreshedDocuments = documentClientService.listDocuments(displayName);
            } catch (Exception refreshException) {
                // The local dropdown can still be updated even if the refresh request fails.
            }

            List<DocumentSummaryDto> finalRefreshedDocuments = refreshedDocuments;
            SwingUtilities.invokeLater(() -> {
                if (documentId.equals(documentSessionService.getDocumentId())) {
                    documentSessionService.clear();
                    if (view != null) {
                        view.showCurrentDocument(documentSessionService);
                    }
                }

                if (view != null) {
                    if (finalRefreshedDocuments == null) {
                        view.removeDocumentFromList(documentId);
                    } else {
                        view.showDocumentList(finalRefreshedDocuments);
                    }
                    view.showStatus("Deleted document: " + title);
                }
            });
        } catch (Exception exception) {
            handleDeleteException(displayName, exception);
        }
    }

    private String buildDocumentTitleFromFile(File file) {
        if (file == null || file.getName() == null || file.getName().isBlank()) {
            return "Untitled Document";
        }

        String fileName = file.getName();
        if (fileName.toLowerCase().endsWith(".txt")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        return fileName.isBlank() ? "Untitled Document" : fileName;
    }

    public void openDocumentByCode(String serverUrl, String userId, String displayName, String shareCode) {
        try {
            updateServerUrl(serverUrl);
            String safeUserId = requireText(userId, "User ID");
            String safeDisplayName = requireText(displayName, "Display name");
            String safeShareCode = requireText(shareCode, "Share code");

            OpenDocumentRequest request = new OpenDocumentRequest(null, new UserId(safeUserId), safeDisplayName, safeShareCode);
            OpenDocumentResponse response = documentClientService.openDocumentByCode(request);
            documentSessionService.loadFromOpenResponse(response);

            updateViewAfterOpen("Document opened by share code.");
        } catch (Exception exception) {
            showError("Open by share code failed", exception);
        }
    }

    public void openDocumentByCodeAndOpenEditor(String serverUrl, String displayName, String shareCode) {
        try {
            updateServerUrl(serverUrl);
            String safeDisplayName = requireText(displayName, "Display name");
            String safeShareCode = requireText(shareCode, "Share code");

            if (view != null) {
                view.showStatus("Opening document by share code...");
            }

            // Run HTTP + WebSocket work on a background thread to avoid freezing the UI
            new Thread(() -> {
                try {
                    OpenDocumentRequest request = new OpenDocumentRequest(
                            null,
                            new UserId(safeDisplayName),
                            safeDisplayName,
                            safeShareCode
                    );
                    OpenDocumentResponse response = documentClientService.openDocumentByCode(request);
                    documentSessionService.loadFromOpenResponse(response);

                    SwingUtilities.invokeLater(() -> {
                        if (view != null) {
                            view.showCurrentDocument(documentSessionService);
                            view.showStatus("Document opened. Joining live session...");
                        }
                    });

                    openCurrentDocumentInEditor(serverUrl, safeDisplayName, safeDisplayName);
                } catch (Exception exception) {
                    SwingUtilities.invokeLater(() -> showError("Open by share code failed", exception));
                }
            }, "open-by-code-thread").start();
        } catch (Exception exception) {
            showError("Open by share code failed", exception);
        }
    }

    public void showShareCodes(String serverUrl, String requesterUserId) {
        try {
            updateServerUrl(serverUrl);
            if (!documentSessionService.hasOpenDocument()) {
                throw new IllegalArgumentException("Open or create a document first.");
            }
            if (!documentSessionService.isCanShare()) {
                throw new IllegalArgumentException("Only the owner can view share codes.");
            }

            String safeUserId = requireText(requesterUserId, "User ID");
            ShareCodeResponse response = documentClientService.getShareCodes(
                    documentSessionService.getDocumentId(),
                    safeUserId
            );

            if (view != null) {
                view.showShareCodes(response);
            }
        } catch (Exception exception) {
            showError("Show share codes failed", exception);
        }
    }

    /**
     * Connects WebSocket and sends join request.
     * This method may be called from either a background thread or the EDT.
     * The WebSocket connect() call blocks until connected, so it must NOT run on the EDT.
     */
    public void openCurrentDocumentInEditor(String serverUrl, String userId, String displayName) {
        try {
            updateServerUrl(serverUrl);
            if (!documentSessionService.hasOpenDocument()) {
                throw new IllegalArgumentException("Please create or open a document first.");
            }

            String currentDocumentId = documentSessionService.getDocumentId();
            if (joining) {
                SwingUtilities.invokeLater(() -> {
                    if (view != null) {
                        view.showStatus("Already joining the live session.");
                    }
                });
                return;
            }

            if (editorOpen) {
                if (currentDocumentId != null && currentDocumentId.equals(liveDocumentId)) {
                    SwingUtilities.invokeLater(() -> {
                        focusExistingEditor();
                        if (view != null) {
                            view.showStatus("Editor is already open for this document.");
                        }
                    });
                    return;
                }

                throw new IllegalArgumentException("An editor is already open. Please close it before opening another document.");
            }

            joiningUserId = requireText(userId, "User ID");
            joiningDisplayName = requireText(displayName, "Display name");
            joining = true;
            liveDocumentId = currentDocumentId;

            SwingUtilities.invokeLater(() -> {
                if (view != null) {
                    view.setJoiningLiveSession(true);
                    view.showStatus("Connecting to live session for document " + currentDocumentId + "...");
                }
            });

            sessionController.setServerBaseUrl(config.getServerBaseUrl());
            if (!sessionController.connect()) {
                joining = false;
                liveDocumentId = null;
                SwingUtilities.invokeLater(() -> {
                    if (view != null) {
                        view.setOpenEditorButtonReady(documentSessionService.hasOpenDocument());
                    }
                });
                return;
            }

            sessionController.joinDocumentSession(
                    currentDocumentId,
                    joiningUserId,
                    joiningDisplayName,
                    documentSessionService.getRole()
            );
        } catch (Exception exception) {
            SwingUtilities.invokeLater(() -> showError("Could not open live editor", exception));
        }
    }

    private void updateServerUrl(String serverUrl) {
        config.setServerBaseUrl(serverUrl);
        documentClientService.setServerBaseUrl(config.getServerBaseUrl());
    }

    private void updateViewAfterOpen(String message) {
        if (view != null) {
            view.showCurrentDocument(documentSessionService);
            view.showSuccess(message);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            if ("Display name".equals(fieldName)) {
                throw new IllegalArgumentException("Please enter your display name.");
            }
            if ("Document title".equals(fieldName)) {
                throw new IllegalArgumentException("Please enter a document title.");
            }
            if ("Share code".equals(fieldName)) {
                throw new IllegalArgumentException("Please enter a share code.");
            }
            throw new IllegalArgumentException("Please enter " + fieldName + ".");
        }
        return value.trim();
    }

    private void showError(String title, Exception exception) {
        String message = describeException(exception);

        if (view != null) {
            view.showStatus(title + ": " + message);
            view.showError(title, message);
        }
    }

    private void showErrorMessage(String title, String message) {
        if (view != null) {
            view.showStatus(message);
            view.showError(title, message);
        }
    }

    private String describeException(Exception exception) {
        if (exception instanceof ClientApiException) {
            ClientApiException apiException = (ClientApiException) exception;
            return "HTTP " + apiException.getStatusCode() + "\n" + apiException.getResponseBody();
        }

        return exception.getMessage() == null ? exception.toString() : exception.getMessage();
    }

    private void handleDeleteException(String displayName, Exception exception) {
        if (isDocumentNotFoundError(exception)) {
            List<DocumentSummaryDto> refreshedDocuments = null;
            try {
                refreshedDocuments = documentClientService.listDocuments(displayName);
            } catch (Exception refreshException) {
                // Keep the original not-found message visible if the refresh also fails.
            }

            List<DocumentSummaryDto> finalRefreshedDocuments = refreshedDocuments;
            SwingUtilities.invokeLater(() -> {
                if (view == null) {
                    return;
                }
                if (finalRefreshedDocuments != null) {
                    view.showDocumentList(finalRefreshedDocuments);
                }
                String message = "Document was not found. Refreshing your document list.";
                view.showStatus(message);
                view.showError("Delete document failed", message);
            });
            return;
        }

        String message;
        if (isPermissionDeniedError(exception)) {
            message = "You can only delete documents you own.";
        } else {
            message = "Could not delete document: " + describeDeleteException(exception);
        }

        String finalMessage = message;
        SwingUtilities.invokeLater(() -> {
            if (view != null) {
                view.showStatus(finalMessage);
                view.showError("Delete document failed", finalMessage);
            }
        });
    }

    private boolean isPermissionDeniedError(Exception exception) {
        return isApiError(exception, 403, "PERMISSION_DENIED");
    }

    private boolean isDocumentNotFoundError(Exception exception) {
        return isApiError(exception, 404, "DOCUMENT_NOT_FOUND");
    }

    private boolean isApiError(Exception exception, int statusCode, String errorCode) {
        if (!(exception instanceof ClientApiException)) {
            return false;
        }

        ClientApiException apiException = (ClientApiException) exception;
        if (apiException.getStatusCode() == statusCode) {
            return true;
        }

        String responseBody = apiException.getResponseBody();
        return responseBody != null && responseBody.contains(errorCode);
    }

    private String describeDeleteException(Exception exception) {
        if (exception instanceof ClientApiException) {
            ClientApiException apiException = (ClientApiException) exception;
            return "server returned HTTP " + apiException.getStatusCode();
        }

        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.toString() : message;
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        SwingUtilities.invokeLater(() -> {
            if (view != null) {
                view.showStatus("Live connection state: " + state.name());
            }
            if (editorView != null) {
                editorView.updateConnectionState(state);
            }
        });
    }

    @Override
    public void onJoinResponse(JoinSessionResponse response) {
        if (response.isAccepted()) {
            joining = false;
            openEditorWindow();
        } else {
            joining = false;
            liveDocumentId = null;
            statusAfterEditorOpen = null;
            String message = response.getMessage() == null ? "Join was rejected." : response.getMessage();
            SwingUtilities.invokeLater(() -> {
                if (view != null) {
                    view.setOpenEditorButtonReady(documentSessionService.hasOpenDocument());
                    view.showStatus("Join rejected: " + message);
                    view.showError("Join rejected", message);
                }
            });
        }
    }

    @Override
    public void onPresenceUpdate(PresenceUpdateMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorView == null || message == null || message.getUserId() == null) {
                return;
            }

            String user = message.getUserId().getValue();
            String eventName = message.getEventType() != null
                    ? message.getEventType().name()
                    : message.getPresence();

            if ("JOINED".equals(eventName) || "ONLINE".equals(eventName)) {
                editorView.addActiveUser(user);
            } else if ("LEFT".equals(eventName) || "OFFLINE".equals(eventName)) {
                editorView.removeActiveUser(user);
                if (editorController != null) {
                    editorController.removeRemoteCursor(user);
                }
            }
        });
    }

    @Override
    public void onCursorUpdate(CursorUpdateMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorController != null) {
                editorController.handleRemoteCursor(message);
            }
        });
    }

    @Override
    public void onOperation(OperationMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorController != null) {
                editorController.handleRemoteOperation(message);
            }
        });
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        joining = false;
        if (!editorOpen) {
            liveDocumentId = null;
            statusAfterEditorOpen = null;
        }
        String message = errorMessage == null || errorMessage.getMessage() == null
                ? "WebSocket error."
                : errorMessage.getMessage();
        SwingUtilities.invokeLater(() -> {
            if (view != null) {
                view.setOpenEditorButtonReady(documentSessionService.hasOpenDocument());
                view.showStatus("Live connection error: " + message);
                view.showError("Live connection error", message);
            }
        });
    }

    /**
     * Creates the editor window. Called from the WebSocket callback thread after JOIN_SESSION_RESPONSE.
     * Heavy work (snapshot loading, share code HTTP) runs on the current background thread.
     * All Swing component creation/updates are dispatched to the EDT.
     */
    private void openEditorWindow() {
        if (editorView != null) {
            SwingUtilities.invokeLater(this::focusExistingEditor);
            return;
        }

        // Heavy work: load snapshot into CRDT engine (runs on background thread, not EDT)
        EditorState state = new EditorState(new UserId(joiningUserId), documentSessionService.getDocumentId());
        String loadedText = state.loadSnapshot(documentSessionService.getSnapshot());
        System.out.println("EDITOR: Loaded snapshot. Characters: " + (loadedText == null ? 0 : loadedText.length()));

        // Load share codes via HTTP on background thread before switching to EDT
        EditorController controller = new EditorController(sessionController, state, documentClientService, documentSessionService);

        // Pre-fetch share codes on background thread (HTTP call)
        String editorCode = null;
        String viewerCode = null;
        boolean canShare = documentSessionService.isCanShare();
        if (canShare && !documentSessionService.hasShareCodes()) {
            try {
                ShareCodeResponse response = documentClientService.getShareCodes(
                        documentSessionService.getDocumentId(),
                        joiningUserId
                );
                documentSessionService.loadShareCodes(response);
            } catch (Exception exception) {
                System.out.println("Share codes could not be loaded: " + exception.getMessage());
            }
        }
        if (canShare) {
            editorCode = documentSessionService.getEditorCode();
            viewerCode = documentSessionService.getViewerCode();
        }

        // Capture values for use on the EDT
        final String finalEditorCode = editorCode;
        final String finalViewerCode = viewerCode;
        final ConnectionState connState = sessionController.getConnectionState();

        // All Swing work must happen on the EDT
        SwingUtilities.invokeLater(() -> {
            editorController = controller;
            editorView = new EditorView(editorController, joiningDisplayName);
            editorView.setTitle(buildEditorTitle());
            editorView.setDefaultCloseOperation(EditorView.DISPOSE_ON_CLOSE);
            editorView.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    markEditorClosed();
                }
            });
            editorController.setView(editorView);
            editorView.setDocumentInfo(
                    documentSessionService.getTitle(),
                    documentSessionService.getDocumentId(),
                    documentSessionService.getRole(),
                    documentSessionService.getSnapshotVersion(),
                    documentSessionService.isCanEdit()
            );
            if (canShare) {
                editorView.showOwnerShareCodes(finalEditorCode, finalViewerCode);
            } else {
                editorView.hideShareCodes();
            }
            editorView.updateConnectionState(connState);
            editorView.addActiveUser(joiningDisplayName);
            editorView.setVisible(true);
            editorOpen = true;
            liveDocumentId = documentSessionService.getDocumentId();

            if (view != null) {
                view.setEditorAlreadyOpen(true);
                String status = statusAfterEditorOpen;
                statusAfterEditorOpen = null;
                if (status == null || status.isBlank()) {
                    status = "Editor opened for document " + documentSessionService.getDocumentId() + ".";
                }
                view.showStatus(status);
            }
        });
    }

    private void focusExistingEditor() {
        if (editorView == null) {
            return;
        }
        if (!editorView.isVisible()) {
            editorView.setVisible(true);
        }
        editorView.toFront();
        editorView.requestFocus();
    }

    private void markEditorClosed() {
        sessionController.disconnect();
        editorOpen = false;
        joining = false;
        liveDocumentId = null;
        editorView = null;
        editorController = null;

        if (view != null) {
            view.setOpenEditorButtonReady(documentSessionService.hasOpenDocument());
            view.showStatus("Editor closed. You can open the live session again.");
        }
    }

    private String buildEditorTitle() {
        String title = documentSessionService.getTitle();
        if (title == null || title.isBlank()) {
            title = documentSessionService.getDocumentId();
        }
        return "Collab Editor - " + title + " (" + documentSessionService.getRole() + ")";
    }
}
