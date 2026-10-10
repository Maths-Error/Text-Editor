package com.team.editor.common.crdt.character;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CharNode {
    private final CharId id;
    private final CharId parentId;
    private final char value;
    private final List<CharNode> children;
    private boolean tombstone;
    private boolean bold;
    private boolean italic;
    private boolean underline;

    public CharNode(CharId id, CharId parentId, char value) {
        if (id == null) {
            throw new IllegalArgumentException("CharNode id cannot be null");
        }
        this.id = id;
        this.parentId = parentId;
        this.value = value;
        this.children = new ArrayList<>();
        this.tombstone = false;
        this.bold = false;
        this.italic = false;
        this.underline = false;
    }

    public CharId getId() {
        return id;
    }

    public CharId getParentId() {
        return parentId;
    }

    public char getValue() {
        return value;
    }

    public List<CharNode> getChildren() {
        return children;
    }

    public boolean isTombstone() {
        return tombstone;
    }

    public boolean isVisible() {
        return !tombstone;
    }

    public void markDeleted() {
        this.tombstone = true;
    }

    public void restore() {
        this.tombstone = false;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean hasAnyFormatting() {
        return bold || italic || underline;
    }

    public boolean hasFormat(String format) {
        if ("BOLD".equalsIgnoreCase(format)) {
            return bold;
        }
        if ("ITALIC".equalsIgnoreCase(format)) {
            return italic;
        }
        if ("UNDERLINE".equalsIgnoreCase(format)) {
            return underline;
        }
        return false;
    }

    public void setFormat(String format, boolean enabled) {
        if ("BOLD".equalsIgnoreCase(format)) {
            setBold(enabled);
        } else if ("ITALIC".equalsIgnoreCase(format)) {
            setItalic(enabled);
        } else if ("UNDERLINE".equalsIgnoreCase(format)) {
            setUnderline(enabled);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharNode charNode)) return false;
        return value == charNode.value &&
                tombstone == charNode.tombstone &&
                bold == charNode.bold &&
                italic == charNode.italic &&
                underline == charNode.underline &&
                Objects.equals(id, charNode.id) &&
                Objects.equals(parentId, charNode.parentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, value, tombstone, bold, italic, underline);
    }

    @Override
    public String toString()
    {
        return
        "CharNode{"     +
        "id="           + id +
        ", parentId="   + parentId +
        ", value="      + value +
        ", tombstone="  + tombstone +
        ", bold="       + bold +
        ", italic="     + italic +
        ", underline="  + underline +
        '}';
    }
}
