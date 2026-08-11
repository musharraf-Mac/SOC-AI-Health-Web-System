package com.example.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
@SpringBootApplication
public class AuthApplication {

	@Autowired
	private MongoTemplate mongoTemplate;
	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

	@Bean
	public CommandLineRunner debugMongo(Environment env) {
		return args -> {
			System.out.println("ACTUAL MONGO URI: " + env.getProperty("spring.data.mongodb.uri"));
			System.out.println("DATABASE NAME: " + env.getProperty("spring.data.mongodb.database"));

			String actualDbname = mongoTemplate.getDb().getName();
			System.out.println("MongoTemplate is connected to database: " + actualDbname);
		};
	}
}