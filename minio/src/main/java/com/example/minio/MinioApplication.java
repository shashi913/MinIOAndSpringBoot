package com.example.minio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MinioApplication {

	public static void main(String[] args) {
		SpringApplication.run(MinioApplication.class, args);
	}

}
