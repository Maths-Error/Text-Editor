package com.team.editor.server.repository;

import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import com.team.editor.server.persistence.MongoDocumentRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary repository used by simple unit tests. The running app uses MongoDocumentRepository.
 */
public class InMemoryDocumentRepository implements DocumentRepository {
    private final ConcurrentHashMap<String, MongoDocumentRecord> documents = new ConcurrentHashMap<>();

    @Override
    public void save(MongoDocumentRecord document) {
        documents.put(document.getDocumentId(), document);
    }

    @Override
    public Optional<MongoDocumentRecord> findById(String documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    @Override
    public Optional<MongoDocumentRecord> findByShareCode(String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            return Optional.empty();
        }

        for (MongoDocumentRecord document : documents.values()) {
            if (document.isDeleted()) {
                continue;
            }

            if (shareCode.equals(document.getEditorCode()) || shareCode.equals(document.getViewerCode())) {
                return Optional.of(document);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<MongoDocumentRecord> findByOwnerId(String ownerId) {
        List<MongoDocumentRecord> ownedDocuments = new ArrayList<>();

        for (MongoDocumentRecord document : documents.values()) {
            if (document.isDeleted()) {
                continue;
            }

            if (document.getOwnerId() != null && ownerId.equals(document.getOwnerId().getValue())) {
                ownedDocuments.add(document);
            }
        }

        return ownedDocuments;
    }

    @Override
    public boolean codeExists(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        for (MongoDocumentRecord document : documents.values()) {
            if (code.equals(document.getEditorCode()) || code.equals(document.getViewerCode())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized Optional<MongoDocumentRecord> updateSnapshot(
            String documentId,
            CrdtSnapshotDto snapshot,
            String updatedAt
    ) {
        MongoDocumentRecord document = documents.get(documentId);

        if (document == null || document.isDeleted()) {
            return Optional.empty();
        }

        document.setSnapshot(snapshot);
        document.setSnapshotVersion(document.getSnapshotVersion() + 1);
        document.setUpdatedAt(updatedAt);
        documents.put(documentId, document);

        return Optional.of(document);
    }
}
