package com.team.editor.common.crdt.character;

import com.team.editor.common.model.UserId;
import java.util.*;

public final class CharacterTree {
    private final CharNode virtualRoot;
    private final Map<CharId, CharNode> nodeMap;

    public CharacterTree()
    {
        // Virtual root to hold top-level characters

        this.virtualRoot = new CharNode(new CharId(new UserId("ROOT"), 0), null, '\0');
        this.nodeMap = new HashMap<>();
        this.nodeMap.put(virtualRoot.getId(), virtualRoot);
    }

    public void insert(CharId id, CharId parentId, char value) {
        if (nodeMap.containsKey(id)) return;

        CharNode newNode = new CharNode(id, parentId, value);
        CharNode parent = (parentId == null) ? virtualRoot : nodeMap.get(parentId);

        if (parent == null) {
            throw new IllegalStateException("Parent character " + parentId + " not found in CharacterTree.");
        }

        parent.getChildren().add(newNode);
        nodeMap.put(id, newNode);

        parent.getChildren().sort(CharacterOrdering.SIBLING_ORDER);
    }

    public void delete(CharId id) {
        CharNode node = nodeMap.get(id);
        if (node != null) {
            node.markDeleted();
        }
    }

    public void restore(CharId id) {
        CharNode node = nodeMap.get(id);
        if (node != null) {
            node.restore();
        }
    }

    
    // Performs DFS to generate the visible text (skipping tombstones).
    
    public String generateVisibleText()
    {
        StringBuilder sb = new StringBuilder();
        dfs(virtualRoot, sb);
        return sb.toString();
    }

    private void dfs(CharNode node, StringBuilder sb)
    {
        // Iterative DFS to avoid StackOverflowError on large documents
        Deque<CharNode> stack = new ArrayDeque<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            CharNode current = stack.pop();

            if (current != virtualRoot && current.isVisible()) {
                sb.append(current.getValue());
            }

            // Push children in reverse order so the first child is processed first
            List<CharNode> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    public CharNode getChar(CharId id) { return nodeMap.get(id); }

    public CharNode getVirtualRoot() { return virtualRoot; }
}
