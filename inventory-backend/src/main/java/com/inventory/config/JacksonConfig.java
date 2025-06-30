package com.inventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.StreamReadConstraints; // Re-added
import com.fasterxml.jackson.core.StreamWriteConstraints; // Re-added
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Custom Jackson ObjectMapper configuration for the Spring Boot application.
 * This class configures Jackson to:
 * 1. Correctly handle Hibernate lazy-loading proxies using Hibernate5JakartaModule.
 * This prevents "No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor" errors.
 * 2. Support Java 8 Date and Time API (LocalDateTime, etc.) serialization/deserialization.
 * 3. Configure other useful serialization features like failing on empty beans or self-references.
 * 4. **Increased the maximum JSON nesting depth to accommodate complex object graphs.**
 * This addresses the "Document nesting depth exceeds the maximum allowed" errors.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JsonMapper.Builder builder = JsonMapper.builder()
                .addModule(new Hibernate5JakartaModule())
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        // Manually build mapper and configure constraints after build
        ObjectMapper mapper = builder.build();

        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(2000).build()
        );
        mapper.getFactory().setStreamWriteConstraints(
                StreamWriteConstraints.builder().maxNestingDepth(2000).build()
        );

        return mapper;
    }
}