package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.auth.repository")
public class MongoConfig {
    
    @Bean
    public MongoClient mongoClient() {
        // Force connection to MongoDB
        return MongoClients.create("mongodb://localhost:27017");
    }
    
    @Bean
    public MongoTemplate mongoTemplate() {
       
        return new MongoTemplate(mongoClient(), "t_soc_health_db");
    }
}
