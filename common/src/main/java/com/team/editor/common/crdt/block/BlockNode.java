package com.team.editor.common.crdt.block;
import com.team.editor.common.crdt.character.CharacterTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


 // A node in the Block CRDT Tree [Layer 1]
 // Each block manages its own character-level layer through its innerTree

public final class BlockNode {
    private final BlockId id;
    private final BlockId parentId;
    private final CharacterTree innerTree;
    private final List<BlockNode> children;
    private boolean isDeleted;

    public BlockNode(BlockId id, BlockId parentId)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("BlockId cannot be null");
        }
        this.id = id;
        this.parentId = parentId;
        this.innerTree = new CharacterTree();
        this.children = new ArrayList<>();
        this.isDeleted = false;
    }

    public BlockId getId()         { return id; }
    public boolean isDeleted()       { return isDeleted; }
    public BlockId getParentId()       { return parentId; }
    public CharacterTree getInnerTree()  { return innerTree; }
    public List<BlockNode> getChildren()   { return children; }
    public void setDeleted(boolean deleted)  { isDeleted = deleted; }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof BlockNode that)) return false;

        return
            isDeleted == that.isDeleted &&
            Objects.equals(id, that.id) &&
            Objects.equals(parentId, that.parentId);
    }

    @Override
    public int hashCode() { return Objects.hash(id, parentId, isDeleted); }

    @Override
    public String toString() { return "BlockNode[" + id + ", deleted=" + isDeleted + "]"; }
}
