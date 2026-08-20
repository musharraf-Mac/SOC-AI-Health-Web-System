package com.example.doctor_page.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.doctor_page.repository")
public class MongoConfig {

    @Value("${MONGODB_URI}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        System.out.println("✅✅✅ Connecting to MongoDB Atlas: " + mongoUri);
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }

    private String getDatabaseName() {
        // Extract database name from URI
        if (mongoUri.contains("/")) {
            String[] parts = mongoUri.split("/");
            String lastPart = parts[parts.length - 1];
            if (lastPart.contains("?")) {
                return lastPart.split("\\?")[0];
            }
            return lastPart;
        }
        return "soc_health_db";
    }
}
