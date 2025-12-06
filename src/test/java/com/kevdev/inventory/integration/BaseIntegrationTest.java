package com.kevdev.inventory.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Common base class for integration tests.
 * Uses the "test" profile which is configured to use H2 and no Flyway.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    // No testcontainers or dynamic properties here
}

