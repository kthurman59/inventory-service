package com.kevdev.inventory.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
class PostgresMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventorydb")
                    .withUsername("inventory")
                    .withPassword("inventory");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // make sure you use the Postgres driver in this profile
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        // reuse your normal Flyway setup
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void contextLoads_and_migrations_apply_on_real_postgres() {
        // if the context starts and Flyway runs without exceptions,
        // this test passes. no asserts needed.
    }
}

