package com.jarlium.stock_ml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Configuration
public class StockMlApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockMlApplication.class, args);
	}

	// @Bean
	// public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
	// return args -> {
	// ScrapingService scrapingService = ctx.getBean(ScrapingService.class);
	// scrapingService.checkProductsStock();
	// };
	// }
}
