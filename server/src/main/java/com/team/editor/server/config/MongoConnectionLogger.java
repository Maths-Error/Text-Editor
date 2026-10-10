package com.team.editor.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoConnectionLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoConnectionLogger.class);

    private final MongoTemplate mongoTemplate;

    public MongoConnectionLogger(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseName() {
        LOGGER.info("MongoDB database configured: {}", mongoTemplate.getDb().getName());
    }
}
