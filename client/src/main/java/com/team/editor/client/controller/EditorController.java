package com.team.editor.client.controller;

import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.team.editor.client.file.TextExportService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.client.service.ClientApiException;
import com.team.editor.client.service.DocumentClientService;
import com.team.editor.client.service.DocumentSessionService;
import com.team.editor.client.state.EditorState;
import com.team.editor.client.ui.EditorView;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.ops.block.DeleteBlockOp;
import com.team.editor.common.ops.block.InsertBlockOp;
import com.team.editor.common.ops.character.CharacterFormatState;
import com.team.editor.common.ops.character.DeleteCharOp;
import com.team.editor.common.ops.character.InsertCharOp;
import com.team.editor.common.ops.character.RestoreCharOp;
import com.team.editor.common.protocol.dto.BlockOpPayload;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.message.OperationKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorController {
    private final SessionController sessionController;
    private final EditorState editorState;
    private final DocumentClientService documentClientService;
    private final DocumentSessionService documentSessionService;
    private EditorView editorView;
    private final ObjectMapper mapper;
    private final TextExportService textExportService;
    private boolean typingBold;
    private boolean typingItalic;
    private boolean typingUnderline;

    public EditorController(
            SessionController sessionController,
            EditorState editorState,
            DocumentClientService documentClientService,
            DocumentSessionService documentSessionService
    ) {
        this(sessionController, editorState, documentClientService, documentSessionService, new ObjectMapper());
    }

    public EditorController(SessionController sessionController, EditorState editorState) {
        this(sessionController, editorState, null, null, new ObjectMapper());
    }

    public EditorController(SessionController sessionController, EditorState editorState, ObjectMapper mapper) {
        this(sessionController, editorState, null, null, mapper);
    }

    EditorController(
            SessionController sessionController,
            EditorState editorState,
            DocumentClientService documentClientService,
            DocumentSessionService documentSessionService,
            ObjectMapper mapper
    ) {
        this.sessionController = sessionController;
        this.editorState = editorState;
        this.documentClientService = documentClientService;
        this.documentSessionService = documentSessionService;
        this.mapper = mapper;
        this.textExportService = new TextExportService();
        this.typingBold = false;
        this.typingItalic = false;
        this.typingUnderline = false;
    }

    public void setView(EditorView view) {
        this.editorView = view;
        if (view != null) {
            updateView(null);
        }
    }

    public EditorState getState() {
        return editorState;
    }

    public void saveSnapshot() {
        if (!canSendEditOperations()) {
            if (editorView != null) {
                editorView.showInfo("Viewers cannot save this document.");
            }
            return;
        }

        if (editorView == null) {
            return;
        }

        try {
            String currentText = editorState.getText();
            SaveSnapshotRequest request = new SaveSnapshotRequest(
                    sessionController.getSession().getUserId(),
                    documentSessionService.getSnapshotVersion(),
                    editorState.toSnapshotDto()
            );

            SaveSnapshotResponse response = documentClientService.saveSnapshot(
                    documentSessionService.getDocumentId(),
                    request
            );

            documentSessionService.updateSnapshotVersion(response.getSnapshotVersion());
            if (editorView != null) {
                editorView.updateSnapshotVersion(response.getSnapshotVersion());
                editorView.showInfo("Snapshot saved. Characters: " + currentText.length());
            }
        } catch (ClientApiException e) {
            if (editorView != null) {
                editorView.showError("Save Failed", "Error " + e.getStatusCode() + ": " + e.getResponseBody());
            }
        } catch (Exception e) {
            if (editorView != null) {
                editorView.showError("Save Failed", "An unexpected error occurred: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public void requestUndo() {
        if (!canSendEditOperations()) {
            if (editorView != null) {
                editorView.showInfo("Viewers cannot undo changes.");
            }
            return;
        }
        sessionController.requestUndo();
    }

    public void requestRedo() {
        if (!canSendEditOperations()) {
            if (editorView != null) {
                editorView.showInfo("Viewers cannot redo changes.");
            }
            return;
        }
        sessionController.requestRedo();
    }

    public void insertCharLocal(int offset, char c) {
        if (!canSendEditOperations()) {
            return;
        }

        EditorState.CharPos parentPos = editorState.getParentCharPosForInsert(offset);
        CharId newId = editorState.getEngine().insertChar(parentPos.blockId, parentPos.charId, c);
        CharacterFormatState initialFormatState = currentTypingFormatState();
        editorState.applyFormatStateToCharacter(parentPos.blockId, newId, initialFormatState);

        try {
            InsertCharOp op = new InsertCharOp(newId, parentPos.charId, c, initialFormatState);
            JsonNode payload = mapper.valueToTree(new BlockOpPayload<>(parentPos.blockId, op));

            OperationMessage msg = new OperationMessage(
                    sessionController.getSession().getSessionId(),
                    sessionController.getSession().getUserId(),
                    OperationKind.CHARACTER_INSERT,
                    payload
            );
            sessionController.sendOperation(msg);
            if (initialFormatState.hasAnyFormatting()) {
                updateView(offset + 1);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void insertNewlineLocal(int offset) {
        insertCharLocal(offset, '\n');
    }

    public void deleteCharLocal(int offset) {
        if (!canSendEditOperations()) {
            return;
        }

        EditorState.CharPos pos = editorState.getCharPosAtOffset(offset);
        if (pos.charId == null) {
            return;
        }
        com.team.editor.common.crdt.character.CharNode deletedCharNode = editorState.getVisibleCharNodeAtOffset(offset);
        if (deletedCharNode == null) {
            return;
        }

        editorState.getEngine().deleteChar(pos.blockId, pos.charId);

        try {
            DeleteCharOp op = new DeleteCharOp(
                    pos.charId,
                    deletedCharNode.getParentId(),
                    deletedCharNode.getValue(),
                    editorState.getFormatState(deletedCharNode)
            );
            JsonNode payload = mapper.valueToTree(new BlockOpPayload<>(pos.blockId, op));

            OperationMessage msg = new OperationMessage(
                    sessionController.getSession().getSessionId(),
                    sessionController.getSession().getUserId(),
                    OperationKind.CHARACTER_DELETE,
                    payload
            );
            sessionController.sendOperation(msg);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void pasteTextLocal(int offset, String text) {
        if (!canSendEditOperations()) {
            return;
        }

        int currentOffset = offset;
        for (char c : text.toCharArray()) {
            if (c == '\r') continue;

            EditorState.CharPos parentPos = editorState.getParentCharPosForInsert(currentOffset);
            CharId newId = editorState.getEngine().insertChar(parentPos.blockId, parentPos.charId, c);

            try {
                InsertCharOp op = new InsertCharOp(newId, parentPos.charId, c);
                JsonNode payload = mapper.valueToTree(new BlockOpPayload<>(parentPos.blockId, op));
                OperationMessage msg = new OperationMessage(
                        sessionController.getSession().getSessionId(),
                        sessionController.getSession().getUserId(),
                        OperationKind.CHARACTER_INSERT,
                        payload
                );
                sessionController.sendOperation(msg);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            currentOffset++;
        }
    }

    public void deleteTextLocal(int start, int end) {
        if (!canSendEditOperations()) {
            return;
        }

        for (int i = end - 1; i >= start; i--) {
            EditorState.CharPos pos = editorState.getCharPosAtOffset(i);
            if (pos.charId == null) {
                continue;
            }
            com.team.editor.common.crdt.character.CharNode deletedCharNode = editorState.getVisibleCharNodeAtOffset(i);
            if (deletedCharNode == null) {
                continue;
            }

            editorState.getEngine().deleteChar(pos.blockId, pos.charId);

            try {
                DeleteCharOp op = new DeleteCharOp(
                        pos.charId,
                        deletedCharNode.getParentId(),
                        deletedCharNode.getValue(),
                        editorState.getFormatState(deletedCharNode)
                );
                JsonNode payload = mapper.valueToTree(new BlockOpPayload<>(pos.blockId, op));
                OperationMessage msg = new OperationMessage(
                        sessionController.getSession().getSessionId(),
                        sessionController.getSession().getUserId(),
                        OperationKind.CHARACTER_DELETE,
                        payload
                );
                sessionController.sendOperation(msg);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    public void insertBlockLocal(int offset) {
        if (!canSendEditOperations()) {
            return;
        }

        EditorState.CharPos pos = editorState.getCharPosAtOffset(offset);
        if (pos.charId != null) {
            return;
        }

        try {
            BlockId newBlockId = new BlockId(
                    editorState.getEngine().getUserId(),
                    editorState.getEngine().getBlockClock() + 1
            );
            InsertBlockOp op = new InsertBlockOp(newBlockId, pos.blockId);
            JsonNode payload = mapper.valueToTree(op);
            OperationMessage msg = new OperationMessage(
                    sessionController.getSession().getSessionId(),
                    sessionController.getSession().getUserId(),
                    OperationKind.BLOCK_INSERT,
                    payload
            );
            sessionController.sendOperation(msg);
            editorState.getEngine().applyInsert(op.id(), op.parentId());
            updateView(offset + 1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void handleRemoteOperation(OperationMessage message) {
        if (message == null || sessionController.getSession() == null) {
            return;
        }
        if (message.getSenderId() != null && message.getSenderId().equals(sessionController.getSession().getUserId())) {
            return;
        }

        JsonNode rawPayload = message.getPayload();
        Integer undoRedoCaretPosition = null;

        try {
            JsonNode normalizedPayload = normalizePayload(message.getPayload());
            boolean moveCaretForRequester = isUndoRedoRequestedByCurrentUser(normalizedPayload);

            if (message.getOperationKindEnum() == OperationKind.CHARACTER_INSERT) {
                BlockOpPayload<InsertCharOp> payload = readBlockPayload(normalizedPayload, InsertCharOp.class);
                InsertCharOp op = payload.getOp();
                editorState.getEngine().applyInsertChar(payload.getBlockId(), op.id(), op.parentId(), op.value());
                editorState.applyFormatStateToCharacter(payload.getBlockId(), op.id(), op.formatState());
                if (moveCaretForRequester) {
                    undoRedoCaretPosition = caretAfterVisibleCharacter(payload.getBlockId(), op.id());
                }
            } else if (message.getOperationKindEnum() == OperationKind.CHARACTER_DELETE) {
                BlockOpPayload<DeleteCharOp> payload = readBlockPayload(normalizedPayload, DeleteCharOp.class);
                DeleteCharOp op = payload.getOp();
                int deleteOffset = editorState.getVisibleOffsetForCharacter(payload.getBlockId(), op.id());
                editorState.getEngine().applyDeleteChar(payload.getBlockId(), op.id());
                if (moveCaretForRequester) {
                    undoRedoCaretPosition = clampCaret(deleteOffset >= 0 ? deleteOffset : editorState.getText().length());
                }
            } else if (message.getOperationKindEnum() == OperationKind.CHARACTER_RESTORE) {
                BlockOpPayload<RestoreCharOp> payload = readBlockPayload(normalizedPayload, RestoreCharOp.class);
                RestoreCharOp op = payload.getOp();
                editorState.getEngine().applyRestoreChar(payload.getBlockId(), op.id());
                editorState.applyFormatStateToCharacter(payload.getBlockId(), op.id(), op.formatState());
                if (moveCaretForRequester) {
                    undoRedoCaretPosition = caretAfterVisibleCharacter(payload.getBlockId(), op.id());
                }
            } else if (message.getOperationKindEnum() == OperationKind.BLOCK_INSERT) {
                InsertBlockOp op = mapper.treeToValue(normalizedPayload, InsertBlockOp.class);
                editorState.getEngine().applyInsert(op.id(), op.parentId());
            } else if (message.getOperationKindEnum() == OperationKind.BLOCK_DELETE) {
                DeleteBlockOp op = mapper.treeToValue(normalizedPayload, DeleteBlockOp.class);
                editorState.getEngine().applyDelete(op.id());
            } else if (message.getOperationKindEnum() == OperationKind.FORMAT_APPLY) {
                System.out.println(
                        "REMOTE FORMAT RECEIVED -> kind=APPLY_FORMAT, sender="
                                + senderValue(message)
                                + ", payload="
                                + payloadToLog(normalizedPayload)
                );
                applyRemoteFormat(normalizedPayload, true);
            } else if (message.getOperationKindEnum() == OperationKind.FORMAT_REMOVE) {
                System.out.println(
                        "REMOTE FORMAT RECEIVED -> kind=REMOVE_FORMAT, sender="
                                + senderValue(message)
                                + ", payload="
                                + payloadToLog(normalizedPayload)
                );
                applyRemoteFormat(normalizedPayload, false);
            }
        } catch (Exception e) {
            System.err.println(
                    "Failed to parse/apply remote operation kind=" + message.getOperationKind()
                            + ", payloadNodeType=" + describePayloadShape(rawPayload)
                            + ", payload=" + payloadToLog(rawPayload)
            );
            e.printStackTrace();
        }

        updateView(undoRedoCaretPosition);
        if (undoRedoCaretPosition != null) {
            sendCursorUpdate(undoRedoCaretPosition);
        }
    }

    public void sendCursorUpdate(int offset) {
        if (!canSendCursorUpdates()) {
            return;
        }

        com.team.editor.common.protocol.dto.CursorUpdateMessage msg = new com.team.editor.common.protocol.dto.CursorUpdateMessage(
                sessionController.getSession().getSessionId(),
                sessionController.getSession().getUserId(),
                null,
                offset
        );
        sessionController.sendCursorUpdate(msg);
    }

    public void handleRemoteCursor(com.team.editor.common.protocol.dto.CursorUpdateMessage message) {
        if (sessionController.getSession() == null || message.getUserId().equals(sessionController.getSession().getUserId())) {
            return;
        }
        if (editorView != null) {
            editorView.updateRemoteCursor(message.getUserId().getValue(), message.getCharacterOffset());
        }
    }

    public void removeRemoteCursor(String username) {
        if (editorView != null) {
            editorView.removeRemoteCursor(username);
        }
    }

    public boolean canEdit() {
        return sessionController.getSession() != null
                && sessionController.getSession().getRole() == com.team.editor.client.session.SessionRole.EDITOR;
    }

    private boolean canSendEditOperations() {
        return canEdit();
    }

    private boolean canSendCursorUpdates() {
        return canEdit();
    }

    public void toggleFormat(String format, int startOffset, int endOffset) {
        if (!canSendEditOperations()) {
            if (editorView != null) {
                editorView.showInfo("Viewers cannot format this document.");
            }
            return;
        }

        if (startOffset == endOffset) {
            setTypingFormat(format, !isTypingFormatEnabled(format));
            return;
        }

        int start = Math.min(startOffset, endOffset);
        int end = Math.max(startOffset, endOffset);
        boolean apply = !editorState.isFormatActiveForRange(start, end, format);
        applyFormatRange(start, end, format, apply, true);
    }

    public boolean isFormatButtonSelected(String format, int startOffset, int endOffset) {
        if (startOffset == endOffset) {
            return isTypingFormatEnabled(format);
        }

        int start = Math.min(startOffset, endOffset);
        int end = Math.max(startOffset, endOffset);
        return editorState.isFormatActiveForRange(start, end, format);
    }

    /**
     * Opens a JFileChooser save dialog to export current editor text as a .txt file.
     * Available for all roles (owner, editor, viewer).
     */
    public void exportTextToFile() {
        if (editorView == null) {
            return;
        }

        String currentText = editorView.getText();
        if (currentText == null) {
            currentText = "";
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Text File");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setAcceptAllFileFilterUsed(false);

        // Default filename from document title
        String defaultName = "document";
        if (documentSessionService != null && documentSessionService.getTitle() != null
                && !documentSessionService.getTitle().isBlank()) {
            defaultName = documentSessionService.getTitle();
        }
        chooser.setSelectedFile(new File(defaultName + ".txt"));

        int result = chooser.showSaveDialog(editorView);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null) {
            return;
        }

        // Ensure .txt extension
        if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
            selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
        }

        try {
            textExportService.exportText(selectedFile, currentText);
            editorView.showInfo("Exported text successfully.");
        } catch (IOException e) {
            editorView.showError("Export Failed", "Could not export file: " + e.getMessage());
        } catch (Exception e) {
            editorView.showError("Export Failed", "Could not export file: " + e.getMessage());
        }
    }

    private CharacterFormatState currentTypingFormatState() {
        return new CharacterFormatState(typingBold, typingItalic, typingUnderline);
    }

    private void applyFormatRange(int startOffset, int endOffset, String format, boolean enabled, boolean sendOperation) {
        List<Map<String, Object>> formatTargets = sendOperation
                ? editorState.getFormatTargetsForRange(startOffset, endOffset, format, enabled)
                : List.of();
        editorState.applyFormatToRange(startOffset, endOffset, format, enabled);
        if (sendOperation) {
            sendFormatOperation(startOffset, endOffset, format, enabled, formatTargets);
        }
        updateView(null);
    }

    private void sendFormatOperation(
            int startOffset,
            int endOffset,
            String format,
            boolean enabled,
            List<Map<String, Object>> formatTargets
    ) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("startOffset", startOffset);
            payloadMap.put("endOffset", endOffset);
            payloadMap.put("format", format);
            payloadMap.put("newValue", enabled);
            payloadMap.put("affectedChars", formatTargets);

            OperationMessage msg = new OperationMessage(
                    sessionController.getSession().getSessionId(),
                    sessionController.getSession().getUserId(),
                    enabled ? OperationKind.FORMAT_APPLY : OperationKind.FORMAT_REMOVE,
                    mapper.valueToTree(payloadMap)
            );
            sessionController.sendOperation(msg);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void applyRemoteFormat(JsonNode payload, boolean enabled) {
        String format = payload.path("format").asText();
        if (format.isBlank()) {
            System.err.println("Ignoring remote format operation because field 'format' is missing or blank.");
            return;
        }

        if (payload.has("affectedChars") && payload.get("affectedChars").isArray()) {
            FormatApplyResult result = applyRemoteFormatTargets(payload, format, enabled);
            if (result.appliedCount() > 0) {
                if (result.appliedCount() < result.targetCount()) {
                    System.err.println(
                            "Applied remote format operation to "
                                    + result.appliedCount()
                                    + " of "
                                    + result.targetCount()
                                    + " CRDT character targets."
                    );
                }
                return;
            }
            System.err.println("Remote format operation did not match any CRDT character targets; trying offset fallback.");
            // Older snapshots/tests may have the same visible text but different CRDT ids.
            // Offset fallback keeps those messages usable, while id-based payloads remain the safe path.
        }

        if (!hasValidOffsetRange(payload)) {
            System.err.println("Ignoring remote format operation because it has no usable affectedChars or offset range.");
            return;
        }

        int startOffset = payload.path("startOffset").asInt();
        int endOffset = payload.path("endOffset").asInt();
        editorState.applyFormatToRange(startOffset, endOffset, format, enabled);
    }

    private FormatApplyResult applyRemoteFormatTargets(JsonNode payload, String format, boolean defaultValue) {
        int appliedCount = 0;
        int targetCount = 0;
        for (JsonNode targetNode : payload.get("affectedChars")) {
            targetCount++;
            JsonNode blockIdNode = targetNode.get("blockId");
            JsonNode charIdNode = targetNode.get("charId");
            if (charIdNode == null || !targetNode.has("newValue")) {
                System.err.println("Skipping malformed format target metadata: " + targetNode);
                continue;
            }

            try {
                BlockId blockId = blockIdNode == null || blockIdNode.isNull()
                        ? null
                        : mapper.treeToValue(blockIdNode, BlockId.class);
                CharId charId = mapper.treeToValue(charIdNode, CharId.class);
                boolean newValue = targetNode.path("newValue").asBoolean(defaultValue);
                if (editorState.applyFormatToCharacter(blockId, charId, format, newValue)) {
                    appliedCount++;
                    System.out.println(
                            "REMOTE FORMAT APPLIED -> charId="
                                    + charId
                                    + ", format="
                                    + format
                                    + ", expectedNewValue="
                                    + newValue
                                    + ", actualStoredValue="
                                    + editorState.getFormatValueForCharacter(charId, format)
                    );
                } else {
                    logUnappliedFormatTarget(blockId, charId, format, newValue);
                }
            } catch (JsonProcessingException exception) {
                System.err.println("Failed to parse format target metadata: " + targetNode);
            }
        }
        return new FormatApplyResult(targetCount, appliedCount);
    }

    private boolean hasValidOffsetRange(JsonNode payload) {
        JsonNode startNode = payload.get("startOffset");
        JsonNode endNode = payload.get("endOffset");
        return startNode != null
                && endNode != null
                && startNode.canConvertToInt()
                && endNode.canConvertToInt()
                && startNode.asInt() < endNode.asInt();
    }

    private boolean isUndoRedoRequestedByCurrentUser(JsonNode payload) {
        if (payload == null || sessionController.getSession() == null) {
            return false;
        }

        JsonNode metadata = payload.get("undoRedoMetadata");
        if (metadata == null || !metadata.isObject()) {
            return false;
        }

        JsonNode requestedByNode = metadata.get("requestedByUserId");
        if (requestedByNode == null || !requestedByNode.isTextual()) {
            return false;
        }

        return requestedByNode.asText().equals(sessionController.getSession().getUserId().getValue());
    }

    private Integer caretAfterVisibleCharacter(BlockId blockId, CharId charId) {
        int offset = editorState.getVisibleOffsetForCharacter(blockId, charId);
        if (offset < 0) {
            return clampCaret(editorState.getText().length());
        }
        return clampCaret(offset + 1);
    }

    private int clampCaret(int caretPosition) {
        return Math.max(0, Math.min(caretPosition, editorState.getText().length()));
    }

    private boolean isTypingFormatEnabled(String format) {
        if ("BOLD".equalsIgnoreCase(format)) {
            return typingBold;
        }
        if ("ITALIC".equalsIgnoreCase(format)) {
            return typingItalic;
        }
        if ("UNDERLINE".equalsIgnoreCase(format)) {
            return typingUnderline;
        }
        return false;
    }

    private void setTypingFormat(String format, boolean enabled) {
        if ("BOLD".equalsIgnoreCase(format)) {
            typingBold = enabled;
        } else if ("ITALIC".equalsIgnoreCase(format)) {
            typingItalic = enabled;
        } else if ("UNDERLINE".equalsIgnoreCase(format)) {
            typingUnderline = enabled;
        }
    }

    private void updateView(Integer caretPos) {
        refreshEditorView(caretPos);
    }

    protected void refreshEditorView(Integer caretPos) {
        if (editorView == null) {
            return;
        }

        if (SwingUtilities.isEventDispatchThread()) {
            editorView.refreshText(caretPos);
        } else {
            SwingUtilities.invokeLater(() -> editorView.refreshText(caretPos));
        }
    }

    private <T> BlockOpPayload<T> readBlockPayload(JsonNode payloadNode, Class<T> operationType)
            throws IOException {
        return mapper.readValue(
                mapper.treeAsTokens(payloadNode),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, operationType)
        );
    }

    private JsonNode normalizePayload(JsonNode payloadNode) throws JsonProcessingException {
        if (payloadNode == null || payloadNode.isNull()) {
            throw new IllegalArgumentException("Operation payload is missing.");
        }
        if (payloadNode.isTextual()) {
            return mapper.readTree(payloadNode.asText());
        }
        return payloadNode;
    }

    private String describePayloadShape(JsonNode payloadNode) {
        return payloadNode == null ? "missing" : payloadNode.getNodeType().name();
    }

    private String payloadToLog(JsonNode payloadNode) {
        return payloadNode == null ? "null" : payloadNode.toString();
    }

    private void logUnappliedFormatTarget(BlockId blockId, CharId charId, String format, boolean expectedValue) {
        boolean blockExists = editorState.hasVisibleBlock(blockId);
        boolean charInBlock = editorState.hasCharacterInBlock(blockId, charId);
        boolean charExistsAnywhere = editorState.hasVisibleCharacter(charId);
        System.err.println(
                "REMOTE FORMAT TARGET NOT APPLIED -> charId="
                        + charId
                        + ", blockId="
                        + blockId
                        + ", format="
                        + format
                        + ", expectedNewValue="
                        + expectedValue
                        + ", blockFound="
                        + blockExists
                        + ", charInBlock="
                        + charInBlock
                        + ", charFoundAnywhere="
                        + charExistsAnywhere
        );
    }

    private String senderValue(OperationMessage message) {
        return message.getSenderId() == null ? "<missing>" : message.getSenderId().getValue();
    }

    private String printableChar(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> Character.isISOControl(c) ? "\\u" + String.format("%04x", (int) c) : "'" + c + "'";
        };
    }

    private record FormatApplyResult(int targetCount, int appliedCount) {
    }
}
