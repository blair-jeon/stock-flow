package io.github.blairjeon.stockflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
public class StockflowApplication {
	public static void main(String[] args) {
		SpringApplication.run(StockflowApplication.class, args);
	}
}