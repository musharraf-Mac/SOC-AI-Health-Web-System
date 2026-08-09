package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.github.cdimascio.dotenv.Dotenv;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.auth.repository")
public class MongoConfig {
    private final Dotenv dotenv = Dotenv.load();

    private final String db_name = dotenv.get("DATABASE");
    private final String mongoUri = dotenv.get("MONGODB_URI");
    @Bean
    public MongoClient mongoClient() {
        
        // Force connection to MongoDB
        return MongoClients.create(mongoUri);
    }
    
    @Bean
    public MongoTemplate mongoTemplate() {
       
        return new MongoTemplate(mongoClient(), db_name);
    }
}
