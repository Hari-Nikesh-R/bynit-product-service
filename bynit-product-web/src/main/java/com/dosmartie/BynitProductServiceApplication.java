package com.dosmartie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication(scanBasePackages = {"com.dosmartie"})
@EnableMongoAuditing
public class BynitProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BynitProductServiceApplication.class, args);
	}

}
