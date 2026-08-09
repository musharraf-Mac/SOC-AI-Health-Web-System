
package com.example.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {
    
    private final MongoClient mongoClient;
    
    @Bean
    public CommandLineRunner createDatabaseIfNotExists() {
        return args -> {
            try {
                
                MongoDatabase database = mongoClient.getDatabase("tb_soc_health_db");
                
                // Create a test collection to ensure database exists
                database.createCollection("_test_connection");                
                
                database.getCollection("_test_connection").drop();
                
                log.info("tb_soc_health_db verified and ready!");
                
            } catch (Exception e) {
                log.error("Failed to create/verify tb_soc_health_db", e);
            }
        };
    }
}