package com.team.editor.server.config;

import com.team.editor.common.model.UserId;
import org.bson.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB connection settings are read from spring.data.mongodb.uri.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new UserIdWriteConverter());
        converters.add(new UserIdReadConverter());

        return new MongoCustomConversions(converters);
    }

    /**
     * Stores UserId in MongoDB with the same shape used by the API: { value: "omar" }.
     */
    @WritingConverter
    private static class UserIdWriteConverter implements Converter<UserId, Document> {
        @Override
        public Document convert(UserId source) {
            Document document = new Document();
            document.put("value", source.getValue());
            return document;
        }
    }

    @ReadingConverter
    private static class UserIdReadConverter implements Converter<Document, UserId> {
        @Override
        public UserId convert(Document source) {
            Object value = source.get("value");
            if (value == null) {
                return null;
            }

            return new UserId(value.toString());
        }
    }
}
