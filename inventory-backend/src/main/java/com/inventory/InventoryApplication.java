package com.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication is a convenience annotation that adds:
// @Configuration: Tags the class as a source of bean definitions for the application context.
// @EnableAutoConfiguration: Tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings.
// @ComponentScan: Tells Spring to look for other components, configurations, and services in the 'com.inventory' package, allowing it to find controllers, services, etc.
@SpringBootApplication
public class InventoryApplication {

	public static void main(String[] args) {
		// This is the entry point for the Spring Boot application.
		// It runs the application, starting the embedded Tomcat server and initializing the Spring context.
		SpringApplication.run(InventoryApplication.class, args);
		System.out.println("Spring Boot Inventory Backend Started on port 8080!");
		System.out.println("H2 Console available at: http://localhost:8080/h2-console (use JDBC URL: jdbc:h2:mem:inventorydb)");
	}
}
