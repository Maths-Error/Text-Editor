package com.team.editor.common.crdt.character;
import java.util.Objects;
import com.team.editor.common.model.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public final class CharId implements Comparable<CharId>
{
    private final UserId userId;
    private final long clock;

    @JsonCreator
    public CharId(@JsonProperty("userId") UserId userId, @JsonProperty("clock") long clock)
    {
        if (userId == null)
        {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        if (clock < 0)
        {
            throw new IllegalArgumentException("Clock cannot be negative");
        }
        this.userId = userId;
        this.clock = clock;
    }

    public UserId getUserId() { return userId; }

    public long getClock() { return clock; }

    @Override
    public int compareTo(CharId other) {
        if (other == null) {
            throw new IllegalArgumentException("Other CharId cannot be null");
        }

        int clockComparison = Long.compare(this.clock, other.clock);
        if (clockComparison != 0)
        {
            return clockComparison;
        }

        return this.userId.compareTo(other.userId);
    }

    @Override
    public boolean equals(Object any)
    {
        if (this == any) return true;
        if (!(any instanceof CharId charId)) return false;
        return clock == charId.clock && Objects.equals(userId, charId.userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, clock); }

    @Override
    public String toString() {
        return
        "CharId{"  +
        "userId="  + userId +
        ", clock=" + clock +
        '}';
    }
}