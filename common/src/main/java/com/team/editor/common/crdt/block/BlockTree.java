package com.team.editor.common.crdt.block;
import java.util.*;

// Manages the hierarchy of BlockNodes (Layer 1).
//  Implements deterministic conflict resolution and DFS traversal.

public final class BlockTree {
    private final BlockNode virtualRoot;
    private final Map<BlockId, BlockNode> nodeMap;

    public BlockTree()
    {
        // Virtual root to hold top level blocks
        // Virtual roots ID must be distinct or null

        this.virtualRoot = new BlockNode(new BlockId(new com.team.editor.common.model.UserId("ROOT"), -1), null);
        this.nodeMap = new HashMap<>();
        this.nodeMap.put(virtualRoot.getId(), virtualRoot);
    }

    // Inserts a block into the tree.
    // Uses Conflict Resolution Rule: Descending clock, then Descending UserId.

    public void insert(BlockId id, BlockId parentId) {
        if (nodeMap.containsKey(id)) return;

        BlockNode newNode = new BlockNode(id, parentId);
        BlockNode parent = (parentId == null) ? virtualRoot : nodeMap.get(parentId);

        if (parent == null) {
            throw new IllegalStateException("Parent block " + parentId + " not found in BlockTree.");
        }

        parent.getChildren().add(newNode);
        nodeMap.put(id, newNode);

        // Sorting siblings
        parent.getChildren().sort((a, b) -> a.getId().compareTo(b.getId()));
    }


    // Deletes a block [marks as tombstone]

    public void delete(BlockId id)
    {
        BlockNode node = nodeMap.get(id);
        if (node != null) {
            node.setDeleted(true);
        }
    }


    // Performs Depth-First Search to retrieve all non-deleted blocks

    public List<BlockNode> generateVisibleBlocks()
    {
        List<BlockNode> result = new ArrayList<>();
        dfs(virtualRoot, result);
        return result;
    }

    private void dfs(BlockNode node, List<BlockNode> result)
    {
        // Skip adding the virtual root to the result

        if (node != virtualRoot && !node.isDeleted())
        {
            result.add(node);
        }

        // Children are already sorted during insertion

        for (BlockNode child : node.getChildren())
        {
            dfs(child, result);
        }
    }

    public BlockNode getNode(BlockId id) { return nodeMap.get(id); }
    public BlockNode getVirtualRoot()    { return virtualRoot; }
}
