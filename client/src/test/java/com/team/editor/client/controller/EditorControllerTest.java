package com.team.editor.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.editor.client.session.ClientSession;
import com.team.editor.client.session.SessionRole;
import com.team.editor.client.state.EditorState;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.ops.block.InsertBlockOp;
import com.team.editor.common.ops.character.CharacterFormatState;
import com.team.editor.common.ops.character.DeleteCharOp;
import com.team.editor.common.ops.character.InsertCharOp;
import com.team.editor.common.ops.character.RestoreCharOp;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.BlockOpPayload;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.OperationKind;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void enterAtEndOfBlockCreatesRealNewLineAndNextCharsUseTheNewBlock() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-1");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abc");

        RecordingSessionController sessionController = createSessionController("doc-1", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertBlockLocal(3);
        controller.insertCharLocal(4, 'd');
        controller.insertCharLocal(5, 'e');
        controller.insertCharLocal(6, 'f');

        assertEquals("abc\ndef", state.getText());
        assertEquals(4, sessionController.sentOperations().size());
        assertEquals("INSERT_BLOCK", sessionController.sentOperations().get(0).getOperationKind());

        InsertBlockOp insertBlockOp = mapper.treeToValue(
                sessionController.sentOperations().get(0).getPayload(),
                InsertBlockOp.class
        );
        assertEquals(firstBlockId, insertBlockOp.parentId());

        BlockOpPayload<InsertCharOp> insertCharPayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );
        assertEquals(insertBlockOp.id(), insertCharPayload.getBlockId());
        assertNotEquals(firstBlockId, insertCharPayload.getBlockId());
    }

    @Test
    void enterInMiddleOfLineDoesNothingInsteadOfCreatingAFakeSplit() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-2");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abc");
        BlockId secondBlockId = state.getEngine().insert(firstBlockId);
        insertText(state, secondBlockId, "xyz");

        RecordingSessionController sessionController = createSessionController("doc-2", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertBlockLocal(1);

        assertEquals("abc\nxyz", state.getText());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    @Test
    void newlineCharacterCanBeInsertedInMiddleOfText() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-newline-middle");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abcdef");

        EditorController controller = new EditorController(
                createSessionController("doc-newline-middle", "alice"),
                state
        );

        controller.insertNewlineLocal(3);

        assertEquals("abc\ndef", state.getText());
    }

    @Test
    void newlineCharacterCanBeInsertedAtStartOfText() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-newline-start");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abcdef");

        EditorController controller = new EditorController(
                createSessionController("doc-newline-start", "alice"),
                state
        );

        controller.insertNewlineLocal(0);

        assertEquals("\nabcdef", state.getText());
    }

    @Test
    void newlineCharacterCanBeInsertedAtEndOfText() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-newline-end");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abcdef");

        EditorController controller = new EditorController(
                createSessionController("doc-newline-end", "alice"),
                state
        );

        controller.insertNewlineLocal(6);

        assertEquals("abcdef\n", state.getText());
    }

    @Test
    void relayedNewlineCharacterKeepsRemoteClientInSync() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-newline-remote");
        EditorState bobState = new EditorState(new UserId("bob"), "doc-newline-remote");

        RecordingSessionController aliceSession = createSessionController("doc-newline-remote", "alice");
        RecordingSessionController bobSession = createSessionController("doc-newline-remote", "bob");
        EditorController aliceController = new EditorController(aliceSession, aliceState);
        EditorController bobController = new EditorController(bobSession, bobState);

        aliceController.insertCharLocal(0, 'a');
        aliceController.insertCharLocal(1, 'b');
        aliceController.insertCharLocal(2, 'c');
        aliceController.insertCharLocal(3, 'd');
        aliceController.insertCharLocal(4, 'e');
        aliceController.insertCharLocal(5, 'f');
        for (OperationMessage operationMessage : aliceSession.sentOperations()) {
            bobController.handleRemoteOperation(operationMessage);
        }

        aliceController.insertNewlineLocal(3);
        bobController.handleRemoteOperation(aliceSession.sentOperations().get(aliceSession.sentOperations().size() - 1));

        assertEquals("abc\ndef", bobState.getText());
        assertEquals(aliceState.getText(), bobState.getText());
    }

    @Test
    void deleteCharLocalLeavesInterBlockNewlineUntouched() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-3");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "ab");

        BlockId secondBlockId = state.getEngine().insert(firstBlockId);
        insertText(state, secondBlockId, "cd");

        EditorController controller = new EditorController(createSessionController("doc-3", "alice"), state);

        controller.deleteCharLocal(2);

        assertEquals("ab\ncd", state.getText());
    }

    @Test
    void sequentialLocalInsertsBuildTextAtTheDocumentEnd() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-4");
        EditorController controller = new EditorController(createSessionController("doc-4", "alice"), state);

        controller.insertCharLocal(0, 'a');
        controller.insertCharLocal(1, 'b');
        controller.insertCharLocal(2, 'c');

        assertEquals("abc", state.getText());
    }

    @Test
    void enterDoesNotMutateLocalStateWhenInsertBlockSerializationFails() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-5");
        BlockId firstBlockId = state.getFirstBlockId();
        insertText(state, firstBlockId, "abc");

        RecordingSessionController sessionController = createSessionController("doc-5", "alice");
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public <T extends com.fasterxml.jackson.databind.JsonNode> T valueToTree(Object value) throws IllegalArgumentException {
                if (value instanceof InsertBlockOp) {
                    throw new IllegalArgumentException("boom");
                }
                return super.valueToTree(value);
            }
        };
        EditorController controller = new EditorController(sessionController, state, failingMapper);

        controller.insertBlockLocal(3);

        assertEquals("abc", state.getText());
        assertEquals(1, state.getEngine().getVisibleBlocks().size());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    @Test
    void relayedInsertBlockOperationKeepsTwoClientsInSync() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-6");
        EditorState bobState = new EditorState(new UserId("bob"), "doc-6");
        insertText(aliceState, aliceState.getFirstBlockId(), "abc");
        insertText(bobState, bobState.getFirstBlockId(), "abc");

        RecordingSessionController aliceSession = createSessionController("doc-6", "alice");
        RecordingSessionController bobSession = createSessionController("doc-6", "bob");
        EditorController aliceController = new EditorController(aliceSession, aliceState);
        EditorController bobController = new EditorController(bobSession, bobState);

        aliceController.insertBlockLocal(3);
        bobController.handleRemoteOperation(aliceSession.sentOperations().get(0));

        aliceController.insertCharLocal(4, 'd');
        bobController.handleRemoteOperation(aliceSession.sentOperations().get(1));

        assertEquals("abc\nd", aliceState.getText());
        assertEquals(aliceState.getText(), bobState.getText());
    }

    @Test
    void replayedOperationHistoryLetsALateJoinerCatchUpToTheCurrentDocument() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-7");
        RecordingSessionController aliceSession = createSessionController("doc-7", "alice");
        EditorController aliceController = new EditorController(aliceSession, aliceState);

        aliceController.insertCharLocal(0, 'a');
        aliceController.insertCharLocal(1, 'b');
        aliceController.insertCharLocal(2, 'c');
        aliceController.insertBlockLocal(3);
        aliceController.insertCharLocal(4, 'd');
        aliceController.insertCharLocal(5, 'e');
        aliceController.insertCharLocal(6, 'f');

        EditorState bobState = new EditorState(new UserId("bob"), "doc-7");
        RecordingSessionController bobSession = createSessionController("doc-7", "bob");
        EditorController bobController = new EditorController(bobSession, bobState);

        for (OperationMessage operationMessage : aliceSession.sentOperations()) {
            bobController.handleRemoteOperation(operationMessage);
        }

        assertEquals("abc\ndef", bobState.getText());
        assertEquals(aliceState.getText(), bobState.getText());
    }

    @Test
    void deleteCharPayloadIncludesInverseMetadataBeforeLocalDelete() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-delete-metadata");
        RecordingSessionController sessionController = createSessionController("doc-delete-metadata", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertCharLocal(0, 'a');
        controller.insertCharLocal(1, 'b');
        state.applyFormatToRange(1, 2, "BOLD", true);
        controller.deleteCharLocal(1);

        OperationMessage deleteMessage = sessionController.sentOperations().get(2);
        assertEquals("DELETE_CHAR", deleteMessage.getOperationKind());

        BlockOpPayload<DeleteCharOp> deletePayload = mapper.readValue(
                mapper.treeAsTokens(deleteMessage.getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        BlockOpPayload<InsertCharOp> firstInsertPayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(0).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );

        assertEquals('b', deletePayload.getOp().value());
        assertEquals(firstInsertPayload.getOp().id(), deletePayload.getOp().parentId());
        assertTrue(deletePayload.getOp().formatState().bold());
    }

    @Test
    void remoteRestoreCharOperationMakesDeletedCharacterVisibleAgain() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-restore-remote");
        RecordingSessionController aliceSession = createSessionController("doc-restore-remote", "alice");
        EditorController aliceController = new EditorController(aliceSession, aliceState);

        aliceController.insertCharLocal(0, 'x');
        aliceController.deleteCharLocal(0);
        assertEquals("", aliceState.getText());

        BlockOpPayload<DeleteCharOp> deletePayload = mapper.readValue(
                mapper.treeAsTokens(aliceSession.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        RestoreCharOp restoreCharOp = new RestoreCharOp(deletePayload.getOp().id());
        OperationMessage restoreMessage = new OperationMessage(
                "doc-restore-remote",
                new UserId("SERVER"),
                OperationKind.CHARACTER_RESTORE,
                mapper.valueToTree(new BlockOpPayload<>(deletePayload.getBlockId(), restoreCharOp))
        );

        aliceController.handleRemoteOperation(restoreMessage);

        assertEquals("x", aliceState.getText());
    }

    @Test
    void remoteRestoreCharOperationRestoresFormattingMetadata() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-restore-format");
        RecordingSessionController sessionController = createSessionController("doc-restore-format", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertCharLocal(0, 'x');
        controller.deleteCharLocal(0);
        assertEquals("", state.getText());

        BlockOpPayload<DeleteCharOp> deletePayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        RestoreCharOp restoreCharOp = new RestoreCharOp(
                deletePayload.getOp().id(),
                deletePayload.getOp().parentId(),
                deletePayload.getOp().value(),
                new CharacterFormatState(true, false, false)
        );
        OperationMessage restoreMessage = new OperationMessage(
                "doc-restore-format",
                new UserId("SERVER"),
                OperationKind.CHARACTER_RESTORE,
                mapper.valueToTree(new BlockOpPayload<>(deletePayload.getBlockId(), restoreCharOp))
        );

        controller.handleRemoteOperation(restoreMessage);

        assertEquals("x", state.getText());
        assertTrue(state.isFormatActiveAtOffset(0, "BOLD"));
    }

    @Test
    void requesterRedoRestoreCharMovesCaretAfterRestoredCharacterAndSendsCursorUpdate() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-redo-caret");
        RecordingSessionController sessionController = createSessionController("doc-redo-caret", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        controller.insertCharLocal(0, 'x');
        controller.deleteCharLocal(0);

        BlockOpPayload<DeleteCharOp> deletePayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        RestoreCharOp restoreCharOp = new RestoreCharOp(
                deletePayload.getOp().id(),
                deletePayload.getOp().parentId(),
                deletePayload.getOp().value(),
                deletePayload.getOp().formatState()
        );
        OperationMessage redoMessage = new OperationMessage(
                "doc-redo-caret",
                new UserId("SERVER"),
                OperationKind.CHARACTER_RESTORE,
                withUndoRedoMetadata(new BlockOpPayload<>(deletePayload.getBlockId(), restoreCharOp), "alice", "REDO")
        );

        controller.handleRemoteOperation(redoMessage);

        assertEquals("x", state.getText());
        assertEquals(1, controller.lastCaretPosition());
        assertEquals(1, sessionController.sentCursorUpdates().size());
        assertEquals(1, sessionController.sentCursorUpdates().get(0).getCharacterOffset());
    }

    @Test
    void nonRequesterUndoRedoOperationDoesNotMoveLocalCaretOrSendCursorUpdate() throws Exception {
        EditorState state = new EditorState(new UserId("bob"), "doc-non-requester-caret");
        RecordingSessionController sessionController = createSessionController("doc-non-requester-caret", "bob");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        controller.insertCharLocal(0, 'x');
        controller.deleteCharLocal(0);

        BlockOpPayload<DeleteCharOp> deletePayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        RestoreCharOp restoreCharOp = new RestoreCharOp(
                deletePayload.getOp().id(),
                deletePayload.getOp().parentId(),
                deletePayload.getOp().value(),
                deletePayload.getOp().formatState()
        );
        OperationMessage redoMessage = new OperationMessage(
                "doc-non-requester-caret",
                new UserId("SERVER"),
                OperationKind.CHARACTER_RESTORE,
                withUndoRedoMetadata(new BlockOpPayload<>(deletePayload.getBlockId(), restoreCharOp), "alice", "REDO")
        );

        controller.handleRemoteOperation(redoMessage);

        assertEquals("x", state.getText());
        assertEquals(null, controller.lastCaretPosition());
        assertTrue(sessionController.sentCursorUpdates().isEmpty());
    }

    @Test
    void requesterUndoDeleteCharMovesCaretToDeletedCharacterOffsetAndSendsCursorUpdate() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-undo-delete-caret");
        RecordingSessionController sessionController = createSessionController("doc-undo-delete-caret", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        controller.insertCharLocal(0, 'a');
        controller.insertCharLocal(1, 'b');
        BlockOpPayload<InsertCharOp> secondInsertPayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(1).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );
        DeleteCharOp deleteCharOp = new DeleteCharOp(
                secondInsertPayload.getOp().id(),
                secondInsertPayload.getOp().parentId(),
                secondInsertPayload.getOp().value(),
                secondInsertPayload.getOp().formatState()
        );
        OperationMessage undoMessage = new OperationMessage(
                "doc-undo-delete-caret",
                new UserId("SERVER"),
                OperationKind.CHARACTER_DELETE,
                withUndoRedoMetadata(new BlockOpPayload<>(secondInsertPayload.getBlockId(), deleteCharOp), "alice", "UNDO")
        );

        controller.handleRemoteOperation(undoMessage);

        assertEquals("a", state.getText());
        assertEquals(1, controller.lastCaretPosition());
        assertEquals(1, sessionController.sentCursorUpdates().size());
        assertEquals(1, sessionController.sentCursorUpdates().get(0).getCharacterOffset());
    }

    @Test
    void requesterFormattingUndoRedoDoesNotMoveCaretOrSendCursorUpdate() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-caret");
        insertText(state, state.getFirstBlockId(), "ab");
        RecordingSessionController sessionController = createSessionController("doc-format-caret", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        Map<String, Object> payload = new HashMap<>();
        payload.put("format", "BOLD");
        payload.put("newValue", true);
        payload.put("affectedChars", state.getFormatTargetsForRange(0, 1, "BOLD", true));
        JsonNode payloadWithMetadata = withUndoRedoMetadata(payload, "alice", "REDO");

        OperationMessage formatMessage = new OperationMessage(
                "doc-format-caret",
                new UserId("SERVER"),
                OperationKind.FORMAT_APPLY,
                payloadWithMetadata
        );

        controller.handleRemoteOperation(formatMessage);

        assertTrue(state.isFormatActiveAtOffset(0, "BOLD"));
        assertEquals(null, controller.lastCaretPosition());
        assertTrue(sessionController.sentCursorUpdates().isEmpty());
    }


    @Test
    void relayedFormatOperationKeepsTwoClientsInSync() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-format-remote");
        EditorState bobState = new EditorState(new UserId("bob"), "doc-format-remote");
        insertText(aliceState, aliceState.getFirstBlockId(), "hello");
        insertText(bobState, bobState.getFirstBlockId(), "hello");

        RecordingSessionController aliceSession = createSessionController("doc-format-remote", "alice");
        RecordingSessionController bobSession = createSessionController("doc-format-remote", "bob");
        EditorController aliceController = new EditorController(aliceSession, aliceState);
        EditorController bobController = new EditorController(bobSession, bobState);

        aliceController.toggleFormat("BOLD", 0, 5);
        bobController.handleRemoteOperation(aliceSession.sentOperations().get(0));

        assertEquals("APPLY_FORMAT", aliceSession.sentOperations().get(0).getOperationKind());
        assertTrue(aliceState.isFormatActiveForRange(0, 5, "BOLD"));
        assertTrue(bobState.isFormatActiveForRange(0, 5, "BOLD"));

        aliceController.toggleFormat("BOLD", 0, 5);
        bobController.handleRemoteOperation(aliceSession.sentOperations().get(1));

        assertEquals("REMOVE_FORMAT", aliceSession.sentOperations().get(1).getOperationKind());
        assertTrue(aliceState.getFormatRuns().isEmpty());
        assertTrue(bobState.getFormatRuns().isEmpty());
    }

    @Test
    void formatOperationPayloadIncludesPerCharacterUndoMetadata() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-metadata");
        insertText(state, state.getFirstBlockId(), "hello");
        RecordingSessionController sessionController = createSessionController("doc-format-metadata", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.toggleFormat("BOLD", 1, 4);

        OperationMessage formatMessage = sessionController.sentOperations().get(0);
        assertEquals("APPLY_FORMAT", formatMessage.getOperationKind());
        JsonNode payload = formatMessage.getPayload();
        assertEquals(1, payload.get("startOffset").asInt());
        assertEquals(4, payload.get("endOffset").asInt());
        assertEquals("BOLD", payload.get("format").asText());
        assertTrue(payload.get("newValue").asBoolean());
        assertEquals(3, payload.get("affectedChars").size());

        JsonNode firstTarget = payload.get("affectedChars").get(0);
        assertTrue(firstTarget.get("blockId").isObject());
        assertTrue(firstTarget.get("charId").isObject());
        assertTrue(!firstTarget.get("previousValue").asBoolean());
        assertTrue(firstTarget.get("newValue").asBoolean());
    }

    @Test
    void typingWhileBoldIsActiveSendsOneInsertCharWithInitialFormatState() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-typed-bold");
        RecordingSessionController sessionController = createSessionController("doc-typed-bold", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.toggleFormat("BOLD", 0, 0);
        controller.insertCharLocal(0, 'A');

        assertEquals(1, sessionController.sentOperations().size());
        OperationMessage insertMessage = sessionController.sentOperations().get(0);
        assertEquals("INSERT_CHAR", insertMessage.getOperationKind());

        BlockOpPayload<InsertCharOp> insertPayload = mapper.readValue(
                mapper.treeAsTokens(insertMessage.getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );
        assertTrue(insertPayload.getOp().formatState().bold());
        assertTrue(!insertPayload.getOp().formatState().italic());
        assertTrue(state.isFormatActiveAtOffset(0, "BOLD"));
    }

    @Test
    void typingWhileItalicIsActiveSendsOneInsertCharWithInitialFormatState() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-typed-italic");
        RecordingSessionController sessionController = createSessionController("doc-typed-italic", "alice");
        EditorController controller = new EditorController(sessionController, state);

        controller.toggleFormat("ITALIC", 0, 0);
        controller.insertCharLocal(0, 'I');

        assertEquals(1, sessionController.sentOperations().size());
        BlockOpPayload<InsertCharOp> insertPayload = mapper.readValue(
                mapper.treeAsTokens(sessionController.sentOperations().get(0).getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );
        assertTrue(insertPayload.getOp().formatState().italic());
        assertTrue(state.isFormatActiveAtOffset(0, "ITALIC"));
    }

    @Test
    void remoteFormatOperationCanRestoreMixedPerCharacterValues() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-restore");
        insertText(state, state.getFirstBlockId(), "ab");
        state.applyFormatToRange(0, 2, "BOLD", true);

        RecordingSessionController sessionController = createSessionController("doc-format-restore", "alice");
        EditorController controller = new EditorController(sessionController, state);

        List<Map<String, Object>> targets = state.getFormatTargetsForRange(0, 2, "BOLD", false);
        targets.get(1).put("newValue", true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("startOffset", 0);
        payload.put("endOffset", 2);
        payload.put("format", "BOLD");
        payload.put("newValue", false);
        payload.put("affectedChars", targets);

        OperationMessage restoreMessage = new OperationMessage(
                "doc-format-restore",
                new UserId("SERVER"),
                OperationKind.FORMAT_REMOVE,
                mapper.valueToTree(payload)
        );

        controller.handleRemoteOperation(restoreMessage);

        assertTrue(!state.isFormatActiveAtOffset(0, "BOLD"));
        assertTrue(state.isFormatActiveAtOffset(1, "BOLD"));
    }

    @Test
    void serverGeneratedRedoApplyFormatUsesAffectedCharIdsWithoutOffsets() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-redo-client");
        insertText(state, state.getFirstBlockId(), "ab");
        state.applyFormatToRange(0, 2, "BOLD", false);

        RecordingSessionController sessionController = createSessionController("doc-format-redo-client", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        Map<String, Object> payload = new HashMap<>();
        payload.put("format", "BOLD");
        payload.put("newValue", true);
        payload.put("affectedChars", state.getFormatTargetsForRange(0, 2, "BOLD", true));

        OperationMessage redoMessage = new OperationMessage(
                "doc-format-redo-client",
                new UserId("SERVER"),
                OperationKind.FORMAT_APPLY,
                mapper.valueToTree(payload)
        );

        controller.handleRemoteOperation(redoMessage);

        assertTrue(state.isFormatActiveForRange(0, 2, "BOLD"));
        assertEquals(1, controller.refreshCount());
    }

    @Test
    void serverGeneratedRedoApplyFormatFallsBackToCharIdWhenBlockIdIsNotResolvable() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-redo-block-fallback");
        insertText(state, state.getFirstBlockId(), "ab");

        List<Map<String, Object>> targets = state.getFormatTargetsForRange(0, 2, "BOLD", true);
        for (Map<String, Object> target : targets) {
            target.put("blockId", new BlockId(new UserId("missing-block"), 99));
        }

        RecordingSessionController sessionController = createSessionController("doc-format-redo-block-fallback", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        Map<String, Object> payload = new HashMap<>();
        payload.put("format", "BOLD");
        payload.put("newValue", true);
        payload.put("affectedChars", targets);

        OperationMessage redoMessage = new OperationMessage(
                "doc-format-redo-block-fallback",
                new UserId("SERVER"),
                OperationKind.FORMAT_APPLY,
                mapper.valueToTree(payload)
        );

        controller.handleRemoteOperation(redoMessage);

        assertTrue(state.isFormatActiveForRange(0, 2, "BOLD"));
        assertEquals(1, controller.refreshCount());
    }

    @Test
    void malformedRemoteApplyFormatDoesNotCrashOrMutateFormatting() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-malformed");
        insertText(state, state.getFirstBlockId(), "ab");
        RecordingSessionController sessionController = createSessionController("doc-format-malformed", "alice");
        RecordingEditorController controller = new RecordingEditorController(sessionController, state);

        Map<String, Object> badTarget = new HashMap<>();
        badTarget.put("newValue", true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("format", "BOLD");
        payload.put("affectedChars", List.of(badTarget));

        OperationMessage malformedMessage = new OperationMessage(
                "doc-format-malformed",
                new UserId("SERVER"),
                OperationKind.FORMAT_APPLY,
                mapper.valueToTree(payload)
        );

        controller.handleRemoteOperation(malformedMessage);

        assertTrue(!state.isFormatActiveForRange(0, 2, "BOLD"));
        assertEquals(1, controller.refreshCount());
    }

    @Test
    void viewerSessionCannotApplyFormatting() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-viewer-format", "viewer", SessionRole.VIEWER);
        EditorState state = new EditorState(new UserId("viewer"), "doc-viewer-format");
        insertText(state, state.getFirstBlockId(), "hello");
        EditorController controller = new EditorController(sessionController, state);

        controller.toggleFormat("BOLD", 0, 5);

        assertTrue(sessionController.sentOperations().isEmpty());
        assertTrue(state.getFormatRuns().isEmpty());
    }

    @Test
    void handleRemoteOperationAppliesInsertCharFromRealReceivedWireMessage() throws Exception {
        EditorState aliceState = new EditorState(new UserId("alice"), "doc-8");
        EditorState bobState = new EditorState(new UserId("bob"), "doc-8");

        RecordingSessionController aliceSession = createSessionController("doc-8", "alice");
        RecordingSessionController bobSession = createSessionController("doc-8", "bob");
        EditorController aliceController = new EditorController(aliceSession, aliceState);
        EditorController bobController = new EditorController(bobSession, bobState);

        aliceController.insertCharLocal(0, 'a');

        String rawWireMessage = mapper.writeValueAsString(aliceSession.sentOperations().get(0));
        OperationMessage receivedMessage = mapper.readValue(rawWireMessage, OperationMessage.class);
        bobController.handleRemoteOperation(receivedMessage);

        assertEquals("a", bobState.getText());
    }

    @Test
    void editorSessionCanSendCursorUpdate() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-cursor-editor", "editor", SessionRole.EDITOR);
        EditorController controller = new EditorController(
                sessionController,
                new EditorState(new UserId("editor"), "doc-cursor-editor")
        );

        controller.sendCursorUpdate(4);

        assertEquals(1, sessionController.sentCursorUpdates().size());
        assertEquals(4, sessionController.sentCursorUpdates().get(0).getCharacterOffset());
    }

    @Test
    void editorRequestUndoSendsUndoRequestWithoutMutatingState() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-undo", "editor", SessionRole.EDITOR);
        EditorState state = new EditorState(new UserId("editor"), "doc-undo");
        insertText(state, state.getFirstBlockId(), "abc");
        EditorController controller = new EditorController(sessionController, state);

        controller.requestUndo();

        assertEquals("abc", state.getText());
        assertEquals(1, sessionController.undoRequestCount());
        assertEquals(0, sessionController.redoRequestCount());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    @Test
    void editorRequestRedoSendsRedoRequestWithoutMutatingState() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-redo", "editor", SessionRole.EDITOR);
        EditorState state = new EditorState(new UserId("editor"), "doc-redo");
        insertText(state, state.getFirstBlockId(), "abc");
        EditorController controller = new EditorController(sessionController, state);

        controller.requestRedo();

        assertEquals("abc", state.getText());
        assertEquals(0, sessionController.undoRequestCount());
        assertEquals(1, sessionController.redoRequestCount());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    @Test
    void viewerSessionCannotRequestUndoOrRedo() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-viewer-undo", "viewer", SessionRole.VIEWER);
        EditorState state = new EditorState(new UserId("viewer"), "doc-viewer-undo");
        insertText(state, state.getFirstBlockId(), "abc");
        EditorController controller = new EditorController(sessionController, state);

        controller.requestUndo();
        controller.requestRedo();

        assertEquals("abc", state.getText());
        assertEquals(0, sessionController.undoRequestCount());
        assertEquals(0, sessionController.redoRequestCount());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    @Test
    void notJoinedSessionCannotRequestUndoOrRedo() {
        RecordingSessionController sessionController = new RecordingSessionController();
        EditorState state = new EditorState(new UserId("editor"), "doc-not-joined");
        EditorController controller = new EditorController(sessionController, state);

        controller.requestUndo();
        controller.requestRedo();

        assertEquals(0, sessionController.undoRequestCount());
        assertEquals(0, sessionController.redoRequestCount());
    }

    @Test
    void requestUndoPathDoesNotDirectlyMutateEditorState() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-no-local-undo", "editor", SessionRole.EDITOR);
        EditorState state = new EditorState(new UserId("editor"), "doc-no-local-undo");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertCharLocal(0, 'x');
        controller.requestUndo();

        assertEquals("x", state.getText());
        assertEquals(1, sessionController.undoRequestCount());
        assertEquals(1, sessionController.sentOperations().size());
    }

    @Test
    void viewerSessionDoesNotSendCursorUpdate() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-cursor-viewer", "viewer", SessionRole.VIEWER);
        EditorController controller = new EditorController(
                sessionController,
                new EditorState(new UserId("viewer"), "doc-cursor-viewer")
        );

        controller.sendCursorUpdate(4);

        assertTrue(sessionController.sentCursorUpdates().isEmpty());
    }

    @Test
    void viewerSessionCannotSendEditOperationOrMutateLocalState() throws Exception {
        RecordingSessionController sessionController = createSessionController("doc-viewer-edit", "viewer", SessionRole.VIEWER);
        EditorState state = new EditorState(new UserId("viewer"), "doc-viewer-edit");
        EditorController controller = new EditorController(sessionController, state);

        controller.insertCharLocal(0, 'x');
        controller.insertNewlineLocal(0);
        controller.deleteTextLocal(0, 1);

        assertEquals("", state.getText());
        assertTrue(sessionController.sentOperations().isEmpty());
    }

    private RecordingSessionController createSessionController(String sessionId, String username) throws Exception {
        return createSessionController(sessionId, username, SessionRole.EDITOR);
    }

    private RecordingSessionController createSessionController(String sessionId, String username, SessionRole role) throws Exception {
        RecordingSessionController sessionController = new RecordingSessionController();
        Field sessionField = SessionController.class.getDeclaredField("session");
        sessionField.setAccessible(true);
        sessionField.set(
                sessionController,
                new ClientSession(sessionId, new UserId(username), username, role)
        );
        return sessionController;
    }

    private void insertText(EditorState state, BlockId blockId, String text) {
        CharId parentId = null;
        for (char ch : text.toCharArray()) {
            parentId = state.getEngine().insertChar(blockId, parentId, ch);
        }
    }

    private JsonNode withUndoRedoMetadata(Object payload, String requestedByUserId, String action) {
        ObjectNode payloadNode = (ObjectNode) mapper.valueToTree(payload);
        ObjectNode metadataNode = mapper.createObjectNode();
        metadataNode.put("requestedByUserId", requestedByUserId);
        metadataNode.put("action", action);
        payloadNode.set("undoRedoMetadata", metadataNode);
        return payloadNode;
    }

    private static final class RecordingEditorController extends EditorController {
        private int refreshCount;
        private Integer lastCaretPosition;

        private RecordingEditorController(SessionController sessionController, EditorState editorState) {
            super(sessionController, editorState);
        }

        @Override
        protected void refreshEditorView(Integer caretPos) {
            refreshCount++;
            lastCaretPosition = caretPos;
        }

        private int refreshCount() {
            return refreshCount;
        }

        private Integer lastCaretPosition() {
            return lastCaretPosition;
        }
    }

    private static final class RecordingSessionController extends SessionController {
        private final List<OperationMessage> sentOperations = new ArrayList<>();
        private final List<CursorUpdateMessage> sentCursorUpdates = new ArrayList<>();
        private int undoRequestCount;
        private int redoRequestCount;

        @Override
        public void sendOperation(OperationMessage message) {
            sentOperations.add(message);
        }

        @Override
        public void sendCursorUpdate(CursorUpdateMessage message) {
            sentCursorUpdates.add(message);
        }

        @Override
        public void requestUndo() {
            if (getSession() == null || getSession().getRole() != SessionRole.EDITOR) {
                return;
            }
            undoRequestCount++;
        }

        @Override
        public void requestRedo() {
            if (getSession() == null || getSession().getRole() != SessionRole.EDITOR) {
                return;
            }
            redoRequestCount++;
        }

        private List<OperationMessage> sentOperations() {
            return sentOperations;
        }

        private List<CursorUpdateMessage> sentCursorUpdates() {
            return sentCursorUpdates;
        }

        private int undoRequestCount() {
            return undoRequestCount;
        }

        private int redoRequestCount() {
            return redoRequestCount;
        }
    }
}
