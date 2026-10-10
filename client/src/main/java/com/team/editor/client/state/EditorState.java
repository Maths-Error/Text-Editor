package com.team.editor.client.state;

import com.team.editor.common.crdt.block.BlockCrdtEngine;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.crdt.character.CharNode;
import com.team.editor.common.crdt.document.DocumentCrdt;
import com.team.editor.common.model.DocumentId;
import com.team.editor.common.ops.character.CharacterFormatState;
import com.team.editor.common.model.UserId;

import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorState {
    private final DocumentCrdt document;
    private final UserId localUserId;
    private BlockId firstBlockId; 

    public EditorState(UserId localUserId, String sessionId) {
        this.localUserId = localUserId;
        BlockCrdtEngine engine = new BlockCrdtEngine(localUserId);
        this.document = new DocumentCrdt(new DocumentId(sessionId), engine);
        
        // Instead of everyone creating their own random first block, 
        // create a shared "Genesis" block so everyone agrees on the ID.
        this.firstBlockId = new BlockId(new UserId("SYSTEM"), 0);
        engine.applyInsert(this.firstBlockId, null);
    }

    public CrdtSnapshotDto toSnapshotDto() {
        return toSnapshotDto(getText());
    }

    public CrdtSnapshotDto toSnapshotDto(String text) {
        if (text == null) text = "";
        
        List<Object> blocks = new ArrayList<>();
        // Split by newline, keeping empty strings to preserve exact line count
        String[] lines = text.split("\n", -1);
        List<FormatRun> formatRuns = text.equals(getText()) ? getFormatRuns() : new ArrayList<>();
        int lineStartOffset = 0;
        for (int i = 0; i < lines.length; i++) {
            Map<String, Object> blockMap = new HashMap<>();
            blockMap.put("index", i);
            blockMap.put("text", lines[i]);
            blockMap.put("formats", createFormatMapsForLine(formatRuns, lineStartOffset, lines[i].length()));
            blocks.add(blockMap);
            lineStartOffset += lines[i].length() + 1;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("snapshotType", "CLIENT_TEXT_BLOCKS_V1");

        return new CrdtSnapshotDto(1, blocks, metadata);
    }

    public String loadSnapshot(CrdtSnapshotDto snapshot) {
        if (snapshot == null || snapshot.getBlocks() == null || snapshot.getBlocks().isEmpty()) {
            return "";
        }

        List<String> lines = new ArrayList<>();
        List<List<Map<String, Object>>> blockFormats = new ArrayList<>();
        Object typeObj = snapshot.getMetadata() != null ? snapshot.getMetadata().get("snapshotType") : null;
        
        if (!"CLIENT_TEXT_BLOCKS_V1".equals(typeObj)) {
            // Old single-block format or unknown
            Object firstBlock = snapshot.getBlocks().get(0);
            if (firstBlock instanceof Map) {
                Object textObj = ((Map<?, ?>) firstBlock).get("text");
                if (textObj instanceof String) {
                    String text = (String) textObj;
                    String[] splitLines = text.split("\n", -1);
                    for (String line : splitLines) {
                        lines.add(line);
                        blockFormats.add(new ArrayList<>());
                    }
                }
            }
        } else {
            List<Object> blockList = snapshot.getBlocks();
            for (int i = 0; i < blockList.size(); i++) {
                Object blockObj = blockList.get(i);
                if (blockObj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) blockObj;
                    Object textObj = map.get("text");
                    if (textObj instanceof String) {
                        lines.add((String) textObj);
                        blockFormats.add(readFormatMaps(map.get("formats")));
                    }
                }
            }
        }

        if (lines.isEmpty()) {
            return "";
        }

        applyLinesToEngine(lines);
        applyFormatMapsToEngine(blockFormats);
        return String.join("\n", lines);
    }

    private void applyLinesToEngine(List<String> lines) {
        UserId systemUser = new UserId("SYSTEM");
        long globalCharClock = 1;
        long globalBlockClock = 1;
        BlockId previousBlockId = null;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            BlockId currentBlockId;
            if (lineIndex == 0) {
                currentBlockId = getFirstBlockId();
            } else {
                currentBlockId = new BlockId(systemUser, globalBlockClock++);
                getEngine().applyInsert(currentBlockId, previousBlockId);
            }
            previousBlockId = currentBlockId;

            String line = lines.get(lineIndex);
            CharId lastCharId = null;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                CharId charId = new CharId(systemUser, globalCharClock++);
                getEngine().applyInsertChar(currentBlockId, charId, lastCharId, c);
                lastCharId = charId;
            }
        }
        
        getEngine().ensureBlockClockAtLeast(globalBlockClock);
        getEngine().ensureCharacterClockAtLeast(globalCharClock);
    }

    public DocumentCrdt getDocument() {
        return document;
    }
    
    public String getText() {
        return document.getVisibleText();
    }
    
    public BlockCrdtEngine getEngine() {
        return document.getEngine();
    }
    
    public BlockId getFirstBlockId() {
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        if (blocks.isEmpty()) {
            return firstBlockId;
        }
        return blocks.get(0).getId();
    }

    // Helper to get visible characters of a block
    public List<CharNode> getVisibleChars(BlockNode block) {
        List<CharNode> list = new ArrayList<>();
        if (block == null) {
            return list;
        }
        CharNode virtualRoot = block.getInnerTree().getVirtualRoot();
        dfsChars(virtualRoot, virtualRoot, list);
        return list;
    }

    public void applyFormatToRange(int startOffset, int endOffset, String format, boolean enabled) {
        if (format == null || startOffset >= endOffset) {
            return;
        }

        int currentOffset = 0;
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            List<CharNode> chars = getVisibleChars(blocks.get(blockIndex));
            for (CharNode charNode : chars) {
                if (currentOffset >= startOffset && currentOffset < endOffset) {
                    charNode.setFormat(format, enabled);
                }
                currentOffset++;
            }

            if (blockIndex < blocks.size() - 1) {
                currentOffset++;
            }
        }
    }

    public List<Map<String, Object>> getFormatTargetsForRange(
            int startOffset,
            int endOffset,
            String format,
            boolean newValue
    ) {
        List<Map<String, Object>> targets = new ArrayList<>();
        if (format == null || startOffset >= endOffset) {
            return targets;
        }

        int currentOffset = 0;
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BlockNode block = blocks.get(blockIndex);
            List<CharNode> chars = getVisibleChars(block);
            for (CharNode charNode : chars) {
                if (currentOffset >= startOffset && currentOffset < endOffset) {
                    Map<String, Object> target = new HashMap<>();
                    target.put("blockId", block.getId());
                    target.put("charId", charNode.getId());
                    target.put("previousValue", charNode.hasFormat(format));
                    target.put("newValue", newValue);
                    targets.add(target);
                }
                currentOffset++;
            }

            if (blockIndex < blocks.size() - 1) {
                currentOffset++;
            }
        }

        return targets;
    }

    public boolean applyFormatToCharacter(BlockId blockId, CharId charId, String format, boolean enabled) {
        if (charId == null || format == null) {
            return false;
        }

        if (blockId != null) {
            BlockNode block = getEngine().getTree().getNode(blockId);
            if (block != null) {
                CharNode charNode = block.getInnerTree().getChar(charId);
                if (charNode != null) {
                    charNode.setFormat(format, enabled);
                    return true;
                }
            }
        }

        // CharId is globally unique, so this fallback keeps server-generated
        // formatting operations useful even if the client cannot resolve blockId.
        for (BlockNode visibleBlock : getEngine().getVisibleBlocks()) {
            CharNode charNode = visibleBlock.getInnerTree().getChar(charId);
            if (charNode != null) {
                charNode.setFormat(format, enabled);
                return true;
            }
        }

        return false;
    }

    public boolean hasVisibleBlock(BlockId blockId) {
        if (blockId == null) {
            return false;
        }
        return getEngine().getTree().getNode(blockId) != null;
    }

    public boolean hasCharacterInBlock(BlockId blockId, CharId charId) {
        if (blockId == null || charId == null) {
            return false;
        }
        BlockNode block = getEngine().getTree().getNode(blockId);
        return block != null && block.getInnerTree().getChar(charId) != null;
    }

    public boolean hasVisibleCharacter(CharId charId) {
        return findVisibleCharById(charId) != null;
    }

    public boolean getFormatValueForCharacter(CharId charId, String format) {
        CharNode charNode = findVisibleCharById(charId);
        return charNode != null && charNode.hasFormat(format);
    }

    public int getVisibleOffsetForCharacter(BlockId blockId, CharId charId) {
        if (charId == null) {
            return -1;
        }

        int currentOffset = 0;
        for (BlockNode block : getEngine().getVisibleBlocks()) {
            for (CharNode charNode : getVisibleChars(block)) {
                if ((blockId == null || block.getId().equals(blockId)) && charNode.getId().equals(charId)) {
                    return currentOffset;
                }
                currentOffset++;
            }
            currentOffset++;
        }

        currentOffset = 0;
        for (BlockNode block : getEngine().getVisibleBlocks()) {
            for (CharNode charNode : getVisibleChars(block)) {
                if (charNode.getId().equals(charId)) {
                    return currentOffset;
                }
                currentOffset++;
            }
            currentOffset++;
        }

        return -1;
    }

    public CharacterFormatState getFormatStateAtOffset(int offset) {
        CharNode charNode = getVisibleCharAtOffset(offset);
        if (charNode == null) {
            return new CharacterFormatState(false, false, false);
        }
        return getFormatState(charNode);
    }

    public CharacterFormatState getFormatState(CharNode charNode) {
        if (charNode == null) {
            return new CharacterFormatState(false, false, false);
        }
        return new CharacterFormatState(charNode.isBold(), charNode.isItalic(), charNode.isUnderline());
    }

    public boolean applyFormatStateToCharacter(BlockId blockId, CharId charId, CharacterFormatState formatState) {
        if (formatState == null) {
            return false;
        }
        boolean applied = applyFormatToCharacter(blockId, charId, "BOLD", formatState.bold());
        applied = applyFormatToCharacter(blockId, charId, "ITALIC", formatState.italic()) || applied;
        applied = applyFormatToCharacter(blockId, charId, "UNDERLINE", formatState.underline()) || applied;
        return applied;
    }

    private CharNode findVisibleCharById(CharId charId) {
        if (charId == null) {
            return null;
        }
        for (BlockNode visibleBlock : getEngine().getVisibleBlocks()) {
            CharNode charNode = visibleBlock.getInnerTree().getChar(charId);
            if (charNode != null && charNode.isVisible()) {
                return charNode;
            }
        }
        return null;
    }

    public void setFormatAtOffset(int offset, String format, boolean enabled) {
        CharNode charNode = getVisibleCharAtOffset(offset);
        if (charNode != null) {
            charNode.setFormat(format, enabled);
        }
    }

    public boolean isFormatActiveForRange(int startOffset, int endOffset, String format) {
        if (format == null || startOffset >= endOffset) {
            return false;
        }

        boolean sawCharacter = false;
        int currentOffset = 0;
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            List<CharNode> chars = getVisibleChars(blocks.get(blockIndex));
            for (CharNode charNode : chars) {
                if (currentOffset >= startOffset && currentOffset < endOffset) {
                    sawCharacter = true;
                    if (!charNode.hasFormat(format)) {
                        return false;
                    }
                }
                currentOffset++;
            }

            if (blockIndex < blocks.size() - 1) {
                currentOffset++;
            }
        }

        return sawCharacter;
    }

    public boolean isFormatActiveAtOffset(int offset, String format) {
        CharNode charNode = getVisibleCharAtOffset(offset);
        return charNode != null && charNode.hasFormat(format);
    }

    public List<FormatRun> getFormatRuns() {
        List<FormatRun> runs = new ArrayList<>();
        int globalOffset = 0;
        for (BlockNode block : getEngine().getVisibleBlocks()) {
            for (FormatRun localRun : createFormatRuns(getVisibleChars(block))) {
                runs.add(new FormatRun(
                        globalOffset + localRun.getStartOffset(),
                        globalOffset + localRun.getEndOffset(),
                        localRun.isBold(),
                        localRun.isItalic(),
                        localRun.isUnderline()
                ));
            }
            globalOffset += getVisibleChars(block).size() + 1;
        }

        if (!getEngine().getVisibleBlocks().isEmpty()) {
            globalOffset--;
        }
        return runs;
    }

    private CharNode getVisibleCharAtOffset(int targetOffset) {
        if (targetOffset < 0) {
            return null;
        }

        int currentOffset = 0;
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            for (CharNode charNode : getVisibleChars(blocks.get(blockIndex))) {
                if (currentOffset == targetOffset) {
                    return charNode;
                }
                currentOffset++;
            }

            if (blockIndex < blocks.size() - 1) {
                currentOffset++;
            }
        }
        return null;
    }

    public CharNode getVisibleCharNodeAtOffset(int targetOffset) {
        return getVisibleCharAtOffset(targetOffset);
    }

    private List<Map<String, Object>> createFormatMapsForLine(
            List<FormatRun> formatRuns,
            int lineStartOffset,
            int lineLength
    ) {
        List<Map<String, Object>> maps = new ArrayList<>();
        int lineEndOffset = lineStartOffset + lineLength;

        for (FormatRun run : formatRuns) {
            int start = Math.max(run.getStartOffset(), lineStartOffset);
            int end = Math.min(run.getEndOffset(), lineEndOffset);
            if (start < end) {
                maps.add(createFormatMap(new FormatRun(
                        start - lineStartOffset,
                        end - lineStartOffset,
                        run.isBold(),
                        run.isItalic(),
                        run.isUnderline()
                )));
            }
        }

        return maps;
    }

    private Map<String, Object> createFormatMap(FormatRun run) {
        Map<String, Object> map = new HashMap<>();
        map.put("startOffset", run.getStartOffset());
        map.put("endOffset", run.getEndOffset());
        map.put("bold", run.isBold());
        map.put("italic", run.isItalic());
        map.put("underline", run.isUnderline());
        return map;
    }

    private List<FormatRun> createFormatRuns(List<CharNode> chars) {
        List<FormatRun> runs = new ArrayList<>();
        int runStart = -1;
        boolean runBold = false;
        boolean runItalic = false;
        boolean runUnderline = false;

        for (int i = 0; i < chars.size(); i++) {
            CharNode charNode = chars.get(i);
            boolean hasFormatting = charNode.hasAnyFormatting();
            boolean sameAsRun = runStart >= 0
                    && runBold == charNode.isBold()
                    && runItalic == charNode.isItalic()
                    && runUnderline == charNode.isUnderline();

            if (hasFormatting && runStart < 0) {
                runStart = i;
                runBold = charNode.isBold();
                runItalic = charNode.isItalic();
                runUnderline = charNode.isUnderline();
            } else if (hasFormatting && !sameAsRun) {
                runs.add(new FormatRun(runStart, i, runBold, runItalic, runUnderline));
                runStart = i;
                runBold = charNode.isBold();
                runItalic = charNode.isItalic();
                runUnderline = charNode.isUnderline();
            } else if (!hasFormatting && runStart >= 0) {
                runs.add(new FormatRun(runStart, i, runBold, runItalic, runUnderline));
                runStart = -1;
            }
        }

        if (runStart >= 0) {
            runs.add(new FormatRun(runStart, chars.size(), runBold, runItalic, runUnderline));
        }

        return runs;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readFormatMaps(Object value) {
        if (!(value instanceof List<?> values)) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> formatMaps = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Map<?, ?> map) {
                formatMaps.add((Map<String, Object>) map);
            }
        }
        return formatMaps;
    }

    private void applyFormatMapsToEngine(List<List<Map<String, Object>>> blockFormats) {
        List<BlockNode> blocks = getEngine().getVisibleBlocks();
        for (int blockIndex = 0; blockIndex < blocks.size() && blockIndex < blockFormats.size(); blockIndex++) {
            List<CharNode> chars = getVisibleChars(blocks.get(blockIndex));
            for (Map<String, Object> formatMap : blockFormats.get(blockIndex)) {
                int start = readInt(formatMap.get("startOffset"));
                int end = readInt(formatMap.get("endOffset"));
                boolean bold = readBoolean(formatMap.get("bold"));
                boolean italic = readBoolean(formatMap.get("italic"));
                boolean underline = readBoolean(formatMap.get("underline"));

                int safeStart = Math.max(0, start);
                int safeEnd = Math.min(end, chars.size());
                for (int i = safeStart; i < safeEnd; i++) {
                    chars.get(i).setBold(bold);
                    chars.get(i).setItalic(italic);
                    chars.get(i).setUnderline(underline);
                }
            }
        }
    }

    private int readInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private boolean readBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private void dfsChars(CharNode node, CharNode virtualRoot, List<CharNode> list) {
        // Iterative DFS to avoid StackOverflowError on large documents
        java.util.Deque<CharNode> stack = new java.util.ArrayDeque<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            CharNode current = stack.pop();

            if (current != virtualRoot && current.isVisible()) {
                list.add(current);
            }

            // Push children in reverse order so the first child is processed first
            java.util.List<CharNode> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    public CharPos getCharPosAtOffset(int offset) {
        offset = Math.max(0, offset);
        int currentOffset = 0;
        List<BlockNode> blocks = getEngine().getVisibleBlocks();

        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BlockNode block = blocks.get(blockIndex);
            List<CharNode> chars = getVisibleChars(block);

            for (CharNode charNode : chars) {
                if (currentOffset == offset) {
                    return new CharPos(block.getId(), charNode.getId(), currentOffset);
                }
                currentOffset++;
            }

            if (blockIndex < blocks.size() - 1) {
                if (currentOffset == offset) {
                    return new CharPos(block.getId(), null, currentOffset);
                }
                currentOffset++;
            }
        }

        if (!blocks.isEmpty()) {
            BlockNode lastBlock = blocks.get(blocks.size() - 1);
            return new CharPos(lastBlock.getId(), null, currentOffset);
        }

        return new CharPos(firstBlockId, null, 0);
    }
    
    // Gets the parent ID (the character to insert after) for a given offset.
    public CharPos getParentCharPosForInsert(int offset) {
        offset = Math.max(0, offset);
        List<BlockNode> blocks = getEngine().getVisibleBlocks();

        if (blocks.isEmpty()) {
            return new CharPos(firstBlockId, null, 0);
        }

        int currentOffset = 0;
        for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            BlockNode block = blocks.get(blockIndex);
            List<CharNode> chars = getVisibleChars(block);

            if (currentOffset == offset) {
                return new CharPos(block.getId(), null, currentOffset);
            }

            for (CharNode charNode : chars) {
                currentOffset++;
                if (currentOffset == offset) {
                    return new CharPos(block.getId(), charNode.getId(), currentOffset);
                }
            }

            if (blockIndex < blocks.size() - 1) {
                currentOffset++;
            }
        }

        BlockNode lastBlock = blocks.get(blocks.size() - 1);
        List<CharNode> chars = getVisibleChars(lastBlock);
        CharId lastCharId = chars.isEmpty() ? null : chars.get(chars.size() - 1).getId();
        return new CharPos(lastBlock.getId(), lastCharId, currentOffset);
    }

    public static class CharPos {
        public final BlockId blockId;
        public final CharId charId;
        public final int offset;
        
        public CharPos(BlockId blockId, CharId charId, int offset) {
            this.blockId = blockId;
            this.charId = charId;
            this.offset = offset;
        }
    }

    public static class FormatRun {
        private final int startOffset;
        private final int endOffset;
        private final boolean bold;
        private final boolean italic;
        private final boolean underline;

        public FormatRun(int startOffset, int endOffset, boolean bold, boolean italic, boolean underline) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public boolean isBold() {
            return bold;
        }

        public boolean isItalic() {
            return italic;
        }

        public boolean isUnderline() {
            return underline;
        }
    }
}
