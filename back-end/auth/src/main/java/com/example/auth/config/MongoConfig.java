package com.example.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.auth.repository")
public class MongoConfig {
    
    
    @Value("${MONGODB_URI}")
    private String mongoUri;
    
    @Value("${DATABASE}")
    private String db_name;
    
    @Bean
    public MongoClient mongoClient() {
        System.out.println("Connecting to MongoDB: " + mongoUri);
        System.out.println("Using database: " + db_name);
        return MongoClients.create(mongoUri);
    }
    
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), db_name);
    }
}