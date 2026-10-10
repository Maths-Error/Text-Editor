package com.team.editor.common.crdt.character;
import java.util.Comparator;

public final class CharacterOrdering
{
    private CharacterOrdering() { }

    public static final Comparator<CharNode> SIBLING_ORDER = (left, right) ->
    {
        if (left == right) return 0;
        if (left == null || right == null)
        {
            throw new IllegalArgumentException("CharNode arguments cannot be null");
        }

        CharId leftId = left.getId();
        CharId rightId = right.getId();

        int byClockDescending = Long.compare(rightId.getClock(), leftId.getClock());
        if (byClockDescending != 0) {
            return byClockDescending;
        }
        
        int byUserIdDescending = rightId.getUserId().compareTo(leftId.getUserId());
        if (byUserIdDescending != 0) {
            return byUserIdDescending;
        }

        return leftId.compareTo(rightId);
    };

    public static int compareSiblings(CharNode left, CharNode right)
    {
        return SIBLING_ORDER.compare(left, right);
    }
}