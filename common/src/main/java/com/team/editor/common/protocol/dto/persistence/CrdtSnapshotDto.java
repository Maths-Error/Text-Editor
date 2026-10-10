package com.team.editor.common.protocol.dto.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flexible CRDT snapshot DTO. This stores CRDT-form state, not rendered text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrdtSnapshotDto {
    private int formatVersion = 1;
    private List<Object> blocks = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public CrdtSnapshotDto() {
    }

    public CrdtSnapshotDto(int formatVersion, List<Object> blocks, Map<String, Object> metadata) {
        this.formatVersion = formatVersion;
        this.blocks = blocks;
        this.metadata = metadata;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    public List<Object> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Object> blocks) {
        this.blocks = blocks;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
