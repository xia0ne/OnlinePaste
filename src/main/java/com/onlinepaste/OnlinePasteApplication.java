package com.onlinepaste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OnlinePasteApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlinePasteApplication.class, args);
	}

}
