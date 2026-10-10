package com.team.editor.client.cursor;

import java.awt.Color;

/**
 * Represents a remote participant cursor shown in the local editor.
 */
public class RemoteCursor {
    private final String username;
    private final Color color;
    private int offset;

    public RemoteCursor(String username, Color color, int offset) {
        this.username = username;
        this.color = color;
        this.offset = offset;
    }

    public String getUsername() {
        return username;
    }

    public Color getColor() {
        return color;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
