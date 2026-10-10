package com.team.editor.server.repository;

import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import com.team.editor.server.persistence.MongoDocumentRecord;

import java.util.List;
import java.util.Optional;

/**
 * Document metadata persistence operations for Phase 3.
 */
public interface DocumentRepository {
    void save(MongoDocumentRecord document);

    Optional<MongoDocumentRecord> findById(String documentId);

    Optional<MongoDocumentRecord> findByShareCode(String shareCode);

    List<MongoDocumentRecord> findByOwnerId(String ownerId);

    boolean codeExists(String code);

    Optional<MongoDocumentRecord> updateSnapshot(
            String documentId,
            CrdtSnapshotDto snapshot,
            String updatedAt
    );
}
