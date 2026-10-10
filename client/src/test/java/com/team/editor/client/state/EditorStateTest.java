package com.team.editor.client.state;

import com.team.editor.common.crdt.block.BlockCrdtEngine;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorStateTest {

    @Test
    void toSnapshotDtoCapturesCurrentText() {
        EditorState state = new EditorState(new UserId("alice"), "doc-snapshot");
        insertText(state.getEngine(), state.getFirstBlockId(), "Hello World");

        CrdtSnapshotDto snapshot = state.toSnapshotDto();

        assertNotNull(snapshot);
        assertEquals(1, snapshot.getFormatVersion());
        assertEquals(1, snapshot.getBlocks().size());
        Map<String, Object> block = (Map<String, Object>) snapshot.getBlocks().get(0);
        assertEquals("Hello World", block.get("text"));
        assertEquals("CLIENT_TEXT_BLOCKS_V1", snapshot.getMetadata().get("snapshotType"));
    }

    @Test
    void loadSnapshotPopulatesEngineWithText() {
        EditorState state = new EditorState(new UserId("alice"), "doc-load");
        CrdtSnapshotDto snapshot = new EditorState(new UserId("bob"), "doc-temp")
                .toSnapshotDto();
        // Manually set text since bob's is empty
        ((Map<String, Object>) snapshot.getBlocks().get(0)).put("text", "Reloaded Text");

        state.loadSnapshot(snapshot);

        assertEquals("Reloaded Text", state.getText());
    }

    @Test
    void snapshotPreservesMultiLineText() {
        EditorState state = new EditorState(new UserId("alice"), "doc-multiline");
        String originalText = "hello\nworld\nend";
        
        CrdtSnapshotDto snapshot = state.toSnapshotDto(originalText);
        assertEquals(3, snapshot.getBlocks().size());
        
        EditorState newState = new EditorState(new UserId("bob"), "doc-multiline-new");
        newState.loadSnapshot(snapshot);
        
        assertEquals(originalText, newState.getText());
    }

    @Test
    void applyAndRemoveBoldForRange() {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-range");
        insertText(state.getEngine(), state.getFirstBlockId(), "hello");

        state.applyFormatToRange(0, 5, "BOLD", true);
        assertTrue(state.isFormatActiveForRange(0, 5, "BOLD"));

        state.applyFormatToRange(1, 4, "BOLD", false);
        assertTrue(state.isFormatActiveAtOffset(0, "BOLD"));
        assertFalse(state.isFormatActiveAtOffset(1, "BOLD"));
        assertFalse(state.isFormatActiveAtOffset(3, "BOLD"));
        assertTrue(state.isFormatActiveAtOffset(4, "BOLD"));
    }

    @Test
    void applyFormatToCharacterUpdatesFormattingByCrdtCharId() {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-by-id");
        List<CharId> charIds = insertText(state.getEngine(), state.getFirstBlockId(), "ab");

        boolean applied = state.applyFormatToCharacter(
                state.getFirstBlockId(),
                charIds.get(0),
                "BOLD",
                true
        );

        assertTrue(applied);
        assertTrue(state.isFormatActiveAtOffset(0, "BOLD"));
        assertTrue(state.getFormatValueForCharacter(charIds.get(0), "BOLD"));
        assertFalse(state.isFormatActiveAtOffset(1, "BOLD"));
    }

    @Test
    void snapshotSavesAndLoadsFormatting() {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-save");
        insertText(state.getEngine(), state.getFirstBlockId(), "hello world");
        state.applyFormatToRange(0, 5, "BOLD", true);
        state.applyFormatToRange(6, 11, "ITALIC", true);
        state.applyFormatToRange(0, 5, "UNDERLINE", true);

        CrdtSnapshotDto snapshot = state.toSnapshotDto();

        EditorState loadedState = new EditorState(new UserId("bob"), "doc-format-load");
        loadedState.loadSnapshot(snapshot);

        assertEquals("hello world", loadedState.getText());
        assertTrue(loadedState.isFormatActiveForRange(0, 5, "BOLD"));
        assertTrue(loadedState.isFormatActiveForRange(0, 5, "UNDERLINE"));
        assertTrue(loadedState.isFormatActiveForRange(6, 11, "ITALIC"));
        assertFalse(loadedState.isFormatActiveAtOffset(6, "BOLD"));
    }

    @Test
    void snapshotSavesFormattingWhenTextContainsNewlineCharacters() {
        EditorState state = new EditorState(new UserId("alice"), "doc-format-newlines");
        insertText(state.getEngine(), state.getFirstBlockId(), "hello\nworld");
        state.applyFormatToRange(0, 5, "BOLD", true);
        state.applyFormatToRange(6, 11, "ITALIC", true);
        state.applyFormatToRange(0, 11, "UNDERLINE", true);

        CrdtSnapshotDto snapshot = state.toSnapshotDto();

        assertEquals(2, snapshot.getBlocks().size());
        assertEquals(1, getFormats(snapshot, 0).size());
        assertEquals(1, getFormats(snapshot, 1).size());

        EditorState loadedState = new EditorState(new UserId("bob"), "doc-format-newlines-load");
        loadedState.loadSnapshot(snapshot);

        assertEquals("hello\nworld", loadedState.getText());
        assertTrue(loadedState.isFormatActiveForRange(0, 5, "BOLD"));
        assertTrue(loadedState.isFormatActiveForRange(0, 5, "UNDERLINE"));
        assertTrue(loadedState.isFormatActiveForRange(6, 11, "ITALIC"));
        assertTrue(loadedState.isFormatActiveForRange(6, 11, "UNDERLINE"));
        assertFalse(loadedState.isFormatActiveAtOffset(6, "BOLD"));
    }

    @Test
    void oldSnapshotWithoutFormattingStillLoads() {
        EditorState state = new EditorState(new UserId("alice"), "doc-old-format");
        CrdtSnapshotDto snapshot = new EditorState(new UserId("bob"), "doc-temp").toSnapshotDto("plain text");
        ((Map<String, Object>) snapshot.getBlocks().get(0)).remove("formats");

        state.loadSnapshot(snapshot);

        assertEquals("plain text", state.getText());
        assertFalse(state.isFormatActiveAtOffset(0, "BOLD"));
        assertTrue(state.getFormatRuns().isEmpty());
    }

    @Test
    void singleEmptyBlockOffsetsAlignWithVisibleText() {
        EditorState state = new EditorState(new UserId("alice"), "doc-1");
        BlockId firstBlockId = state.getFirstBlockId();

        assertEquals("", state.getText());
        assertCharPos(state.getCharPosAtOffset(0), firstBlockId, null, 0);
        assertCharPos(state.getParentCharPosForInsert(0), firstBlockId, null, 0);
        assertEquals(state.getText().length(), state.getCharPosAtOffset(0).offset);
    }

    @Test
    void singleBlockOffsetsDoNotInventATrailingNewline() {
        EditorState state = new EditorState(new UserId("alice"), "doc-2");
        BlockId firstBlockId = state.getFirstBlockId();
        List<CharId> chars = insertText(state.getEngine(), firstBlockId, "abc");

        assertEquals("abc", state.getText());
        assertCharPos(state.getCharPosAtOffset(0), firstBlockId, chars.get(0), 0);
        assertCharPos(state.getCharPosAtOffset(1), firstBlockId, chars.get(1), 1);
        assertCharPos(state.getCharPosAtOffset(2), firstBlockId, chars.get(2), 2);
        assertCharPos(state.getCharPosAtOffset(3), firstBlockId, null, 3);
        assertEquals(state.getText().length(), state.getCharPosAtOffset(3).offset);
    }

    @Test
    void twoBlockOffsetsMatchRenderedTextExactly() {
        TwoBlockFixture fixture = createTwoBlockFixture("ab", "cd");

        assertEquals("ab\ncd", fixture.state().getText());
        assertCharPos(fixture.state().getCharPosAtOffset(0), fixture.first().blockId(), fixture.first().charIds().get(0), 0);
        assertCharPos(fixture.state().getCharPosAtOffset(1), fixture.first().blockId(), fixture.first().charIds().get(1), 1);
        assertCharPos(fixture.state().getCharPosAtOffset(2), fixture.first().blockId(), null, 2);
        assertCharPos(fixture.state().getCharPosAtOffset(3), fixture.second().blockId(), fixture.second().charIds().get(0), 3);
        assertCharPos(fixture.state().getCharPosAtOffset(4), fixture.second().blockId(), fixture.second().charIds().get(1), 4);
        assertCharPos(fixture.state().getCharPosAtOffset(5), fixture.second().blockId(), null, 5);
        assertEquals(fixture.state().getText().length(), fixture.state().getCharPosAtOffset(5).offset);
    }

    @Test
    void insertMappingUsesBlockStartAfterInterBlockNewline() {
        TwoBlockFixture fixture = createTwoBlockFixture("ab", "cd");

        assertCharPos(fixture.state().getParentCharPosForInsert(0), fixture.first().blockId(), null, 0);
        assertCharPos(fixture.state().getParentCharPosForInsert(1), fixture.first().blockId(), fixture.first().charIds().get(0), 1);
        assertCharPos(fixture.state().getParentCharPosForInsert(3), fixture.second().blockId(), null, 3);
        assertCharPos(fixture.state().getParentCharPosForInsert(5), fixture.second().blockId(), fixture.second().charIds().get(1), 5);
    }

    @Test
    void deleteMappingTargetsRealCharactersAndSkipsNewlines() {
        TwoBlockFixture fixture = createTwoBlockFixture("ab", "cd");

        assertCharPos(fixture.state().getCharPosAtOffset(1), fixture.first().blockId(), fixture.first().charIds().get(1), 1);
        assertCharPos(fixture.state().getCharPosAtOffset(4), fixture.second().blockId(), fixture.second().charIds().get(1), 4);
        assertNull(fixture.state().getCharPosAtOffset(2).charId);
    }

    private TwoBlockFixture createTwoBlockFixture(String firstText, String secondText) {
        EditorState state = new EditorState(new UserId("alice"), "doc-two-blocks");
        BlockCrdtEngine engine = state.getEngine();
        BlockId firstBlockId = state.getFirstBlockId();
        List<CharId> firstChars = insertText(engine, firstBlockId, firstText);
        BlockId secondBlockId = engine.insert(firstBlockId);
        List<CharId> secondChars = insertText(engine, secondBlockId, secondText);

        return new TwoBlockFixture(
                state,
                new BlockChars(firstBlockId, firstChars),
                new BlockChars(secondBlockId, secondChars)
        );
    }

    private List<CharId> insertText(BlockCrdtEngine engine, BlockId blockId, String text) {
        List<CharId> charIds = new ArrayList<>();
        CharId parentId = null;
        for (char ch : text.toCharArray()) {
            CharId charId = engine.insertChar(blockId, parentId, ch);
            charIds.add(charId);
            parentId = charId;
        }
        return charIds;
    }

    private void assertCharPos(EditorState.CharPos actual, BlockId expectedBlockId, CharId expectedCharId, int expectedOffset) {
        assertEquals(expectedBlockId, actual.blockId);
        assertEquals(expectedCharId, actual.charId);
        assertEquals(expectedOffset, actual.offset);
    }

    private List<?> getFormats(CrdtSnapshotDto snapshot, int blockIndex) {
        Map<String, Object> block = (Map<String, Object>) snapshot.getBlocks().get(blockIndex);
        return (List<?>) block.get("formats");
    }

    private record BlockChars(BlockId blockId, List<CharId> charIds) {
    }

    private record TwoBlockFixture(EditorState state, BlockChars first, BlockChars second) {
    }

    @Test
    void loadSnapshotHelloWorldRendersCorrectly() {
        // Simulate what happens when a second user opens a document with saved snapshot
        String originalText = "hello\nworld";
        EditorState ownerState = new EditorState(new UserId("owner"), "doc-snap-render");
        CrdtSnapshotDto snapshot = ownerState.toSnapshotDto(originalText);

        // New user loads the snapshot
        EditorState joinerState = new EditorState(new UserId("joiner"), "doc-snap-render");
        String loadedText = joinerState.loadSnapshot(snapshot);

        assertEquals("hello\nworld", loadedText);
        assertEquals("hello\nworld", joinerState.getText());
    }

    @Test
    void loadSnapshotLargeTextDoesNotStackOverflow() {
        // Build a ~5000 character string to verify iterative DFS works
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        String largeText = sb.toString();

        EditorState ownerState = new EditorState(new UserId("owner"), "doc-large");
        CrdtSnapshotDto snapshot = ownerState.toSnapshotDto(largeText);

        EditorState joinerState = new EditorState(new UserId("joiner"), "doc-large");
        String loadedText = joinerState.loadSnapshot(snapshot);

        assertEquals(largeText.length(), loadedText.length());
        assertEquals(largeText, loadedText);
        assertEquals(largeText, joinerState.getText());
    }
}
