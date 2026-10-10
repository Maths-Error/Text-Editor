package com.team.editor.common.crdt.block;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.model.UserId;

import java.util.Objects;

 // Unique identifier for a BlockNode
 // Contains the User ID of the creator and a logical clock

@JsonIgnoreProperties(ignoreUnknown = true)
public final class BlockId implements Comparable<BlockId>
{
    private final UserId userId;
    private final long clock;

    @JsonCreator
    public BlockId(@JsonProperty("userId") UserId userId, @JsonProperty("clock") long clock)
    {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        this.userId = userId;
        this.clock = clock;
    }

    public UserId getUserId() { return userId; }
    public long getClock()    { return clock; }

    @Override
    public int compareTo(BlockId other)
    {
        // Conflict Resolution Rule:
        // 1 -> Primary Sort: Descending clock
        // 2 -> Secondary Sort: Descending UserId
        if (other == null) return 1;
        
        int clockComp = Long.compare(other.clock, this.clock);
        if (clockComp != 0) return clockComp;

        return other.userId.compareTo(this.userId);
    }

    @Override
    public boolean equals(Object any)
    {
        if (this == any) return true;
        if (!(any instanceof BlockId blockId)) return false;
        return clock == blockId.clock && Objects.equals(userId, blockId.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, clock); }

    @Override
    public String toString() { return userId + ":" + clock; }
}
