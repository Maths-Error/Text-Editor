package com.team.editor.server.repository;

import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import com.team.editor.server.persistence.MongoDocumentRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoDocumentRepository implements DocumentRepository {

    private final MongoTemplate mongoTemplate;

    public MongoDocumentRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(MongoDocumentRecord document) {
        mongoTemplate.save(document);
    }

    @Override
    public Optional<MongoDocumentRecord> findById(String documentId) {
        MongoDocumentRecord document = mongoTemplate.findById(documentId, MongoDocumentRecord.class);

        if (document == null) {
            return Optional.empty();
        }

        return Optional.of(document);
    }

    @Override
    public Optional<MongoDocumentRecord> findByShareCode(String shareCode) {
        Criteria codeCriteria = new Criteria().orOperator(
                Criteria.where("editorCode").is(shareCode),
                Criteria.where("viewerCode").is(shareCode)
        );

        Query query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false));
        query.addCriteria(codeCriteria);

        MongoDocumentRecord document = mongoTemplate.findOne(query, MongoDocumentRecord.class);
        if (document == null) {
            return Optional.empty();
        }

        return Optional.of(document);
    }

    @Override
    public List<MongoDocumentRecord> findByOwnerId(String ownerId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerId.value").is(ownerId));
        query.addCriteria(Criteria.where("deleted").is(false));

        return mongoTemplate.find(query, MongoDocumentRecord.class);
    }

    @Override
    public boolean codeExists(String code) {
        Criteria codeCriteria = new Criteria().orOperator(
                Criteria.where("editorCode").is(code),
                Criteria.where("viewerCode").is(code)
        );

        Query query = new Query();
        query.addCriteria(codeCriteria);

        return mongoTemplate.count(query, MongoDocumentRecord.class) > 0;
    }

    @Override
    public Optional<MongoDocumentRecord> updateSnapshot(
            String documentId,
            CrdtSnapshotDto snapshot,
            String updatedAt
    ) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(documentId));
        query.addCriteria(Criteria.where("deleted").is(false));

        Update update = new Update();
        update.set("snapshot", snapshot);
        update.set("updatedAt", updatedAt);
        update.inc("snapshotVersion", 1);

        MongoDocumentRecord updatedDocument = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                MongoDocumentRecord.class
        );

        if (updatedDocument == null) {
            return Optional.empty();
        }

        return Optional.of(updatedDocument);
    }
}
