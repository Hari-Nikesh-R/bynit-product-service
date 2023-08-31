package com.dosmartie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

@SpringBootApplication(scanBasePackages = {"com.dosmartie"})
@EnableMongoAuditing
@EnableFeignClients
public class BynitProductServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(BynitProductServiceApplication.class, args);
	}

}
