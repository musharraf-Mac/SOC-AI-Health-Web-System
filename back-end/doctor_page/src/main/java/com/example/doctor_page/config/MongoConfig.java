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

    @Value("${DATABASE:}")
    private String database;

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        String selectedDatabase = database;
        if (selectedDatabase.isBlank()) selectedDatabase = new com.mongodb.ConnectionString(mongoUri).getDatabase();
        return new MongoTemplate(mongoClient(), selectedDatabase == null || selectedDatabase.isBlank() ? "soc_health_db" : selectedDatabase);
    }

}
