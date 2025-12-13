# Inventory Service
Inventory Service is a Spring Boot microservice that manages product inventory and reservations
for customer orders. It listens to order events, reserves stock for each line item, and
publishes reservation results back to the order domain.
This service is designed to pair with the `order-management-system` service as part of an event
driven architecture.
## Features
• Maintain inventory items and stock levels per location
• Reserve inventory for incoming orders
• Track reservation lines and statuses per order line
• Publish reservation outcomes as events for downstream services
• Flyway based schema migrations
• Testcontainers backed integration tests
• Code coverage enforced with JaCoCo
• GitHub Actions pipeline for build and test
## Architecture Overview
High level responsibilities:
• Consume `orders.created` events from Kafka
• For each order line, locate the matching inventory item and location
• Reserve requested quantity and persist a `Reservation` with `ReservationLine` rows
• Emit an `inventory.reservation.results` event summarizing the outcome per line
• Keep an audit trail through stock adjustment records
Core technical stack:
• Java 17
• Spring Boot 3
• Spring Data JPA and Hibernate
• PostgreSQL for persistence
• Kafka and Spring for Apache Kafka
• Flyway for database migrations
• Testcontainers for integration testing
• Maven, JaCoCo, GitHub Actions
This repository intentionally documents development and testing setup only. It does not
describe production network topology, credentials, or security controls.
## Domain Concepts
• InventoryItem
 Represents on hand stock for a specific product, typically scoped by location and SKU.
• Reservation
 Represents a temporary hold of inventory for an order. Contains status, timestamps, optional
order id, and failure or reason fields.
• ReservationLine
 Represents a reservation per order line, linking to `InventoryItem` and tracking reserved
quantity, requested quantity, location id, SKU, and status.
• StockAdjustment
 Represents changes to stock levels, such as initial load, manual corrections, and reservation
based adjustments.
## Event Contracts
The service participates in two Kafka topics.
• `orders.created`
 Consumed. Carries newly created orders and their line items. The payload format mirrors the
order service contract and is kept within the codebase rather than documented publicly.
• `inventory.reservation.results`
 Produced. Carries reservation outcomes per order and per line. Includes enough information
for the order domain to decide whether to proceed, back order, or cancel, without exposing
internal database identifiers.
These topics and payloads are documented in code level DTOs and internal API documentation, not
in this public README, to avoid leaking unnecessary structural details.
## Getting Started
### Prerequisites
For local development you need:
• Java 17
• Maven wrapper included in this repository
• Docker and Docker Compose if you want to use containers for PostgreSQL and Kafka
• A running PostgreSQL instance reachable from the service
• A running Kafka broker reachable from the service
Values such as hostnames, ports, and credentials should be configured through application
properties or environment variables and are not hard coded.
### Running tests
To run the full test suite with coverage:
./mvnw clean verify
This will:
• Run unit and integration tests
• Start Testcontainers for PostgreSQL integration tests
• Execute Flyway migrations against the test database
• Generate a JaCoCo coverage report and enforce coverage rules
### Running the service locally
You can start the service with:
./mvnw spring-boot:run
You must provide configuration that points to your local PostgreSQL and Kafka instances. For
example, through `application-local.yml` and an active profile, or through environment
variables. Do not commit real credentials or production connection strings.
Example environment variables (placeholders only):
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/inventorydb
SPRING_DATASOURCE_USERNAME=your_username
SPRING_DATASOURCE_PASSWORD=your_password
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
INVENTORY_KAFKA_ENABLED=true
These values are examples for local use only. Use secure secret management and environment
specific configuration for real deployments.
### Database migrations
Database schema changes are managed by Flyway migrations located in:
src/main/resources/db/migration
Migrations are applied automatically on startup in non production profiles and as part of the
integration test suite. Production rollout should use controlled migration procedures and
locked down credentials, which are out of scope for this README.
### Configuration
Key configuration areas:
• Data source and JPA properties
• Kafka producer and consumer properties
• Inventory specific settings such as reservation time to live
Configuration is provided through Spring Boot properties files and environment variables. Do
not store secrets in version control.
### Observability
The service uses Spring Boot Actuator for basic health checks. Typical endpoints used in a
Kubernetes or container orchestrated environment include readiness and liveness checks.
The exact actuator exposure, security, and network configuration should be controlled per
environment and is not documented here.
### CI and Coverage
A GitHub Actions workflow runs the Maven verify phase on each push and pull request to the main
branch. The pipeline:
• Builds the project
• Runs tests and integration tests
• Enforces JaCoCo coverage thresholds
• Fails the build if coverage drops below the configured minimum
### Project Structure
High level module layout:
src
 main
 java/com/kevdev/inventory
 config Spring configuration including Kafka
 controller REST endpoints
 entity JPA entities
 messaging Kafka listeners and event payloads
 repository Spring Data repositories
 service Business logic
 resources
 db/migration Flyway migrations
 application.yml Application configuration
 test
 java/com/kevdev/inventory
 controller Controller tests
 integration Integration tests with Testcontainers
 service Service level tests
 resources
 application-test.yml Test configuration
### Security
This repository:
• Does not contain credentials, API keys, or production connection strings
• Does not document internal production hostnames or ports
• Focuses on local development and testing flows
For any deployment beyond local development you should add:
• Network security and access control
• Secrets management with a dedicated tool
• Authentication and authorization for external interfaces
• Hardened Kafka and PostgreSQL configurations
## License
Choose and add a license file appropriate for your use case, for example Apache 2 or MIT.
