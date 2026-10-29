# 🤖 AGENTS.md — Working Agreement for the Home Energy Tracker Monorepo

> **Audience:** AI coding agents (and humans) contributing to this repository.
> **Scope:** the whole `ENERGY_TRACKER` monorepo. Service-specific details live in each
> service's own `README.md`; this file defines the cross-cutting rules that apply everywhere.

---

## 📑 Table of Contents

- [1. Purpose of this file](#1-purpose-of-this-file)
- [2. Project overview](#2-project-overview)
- [3. Repository layout](#3-repository-layout)
- [4. Tech stack & versions](#4-tech-stack--versions)
- [5. Package & layering conventions](#5-package--layering-conventions)
- [6. Mandatory coding rules](#6-mandatory-coding-rules)
- [7. Mandatory testing rule](#7-mandatory-testing-rule)
- [8. Mandatory documentation rule](#8-mandatory-documentation-rule)
- [9. Cross-cutting patterns to reuse](#9-cross-cutting-patterns-to-reuse)
- [10. Build, run & verify](#10-build-run--verify)
- [11. Do / Don't checklist](#11-do--dont-checklist)
- [12. Known gaps & technical debt](#12-known-gaps--technical-debt)

---

## 1. Purpose of this file

This document is the **single source of truth for how changes must be made** in this repository.
Before writing code, an agent must read:

1. this `AGENTS.md` (global rules), and
2. the `README.md` of the **specific microservice** being modified (module contract, endpoints,
   configuration, current test inventory).

If a rule here conflicts with an explicit instruction from the user, the user wins — but the
deviation must be called out in the final response.

---

## 2. Project overview

**Home Energy Tracker** is a distributed, event-driven platform for tracking home energy
consumption. It is built as a **set of independent Spring Boot microservices** that communicate
over REST and Apache Kafka, each owning its own data store.

| Service | Port | Package root | Primary storage | Role |
| :--- | :---: | :--- | :--- | :--- |
| **user-service** | `8080` | `com.pgs.user.service` | MySQL + Redis | CRUD users, alerting flag, energy threshold, dynamic filtering/pagination |
| **device-service** | `8081` | `com.pgs.device.service` | MySQL + Redis | CRUD devices, device types, devices per user |
| **ingestion-service** | `8082` | `com.pgs.ingestion.service` | Stateless | Validates IoT readings and publishes `EnergyUsageEvent` to Kafka (fire-and-forget) |
| **usage-service** | `8083` | `com.pgs.usage.service` | InfluxDB | Consumes readings, `@Scheduled` aggregation, threshold checks, publishes `AlertingEvent` |
| **alert-service** | `8084` | `com.pgs.alert.service` | MySQL | Consumes `AlertingEvent`, sends email via SMTP, persists audit record |
| **insight-service** | `8085` | `com.pgs.insight.service` | Stateless | Pulls aggregated usage, orchestrates Ollama LLM prompts via Spring AI |
| **api-gateway** | `9000` | `com.pgs.api.gateway` | Stateless | Spring Cloud Gateway (WebMVC) routes + Resilience4j circuit breakers |

**Kafka topics:** `energy-usage` (readings), `energy-alerts` (alerts), `energy-alerts-dlt`
(dead-letter).

### Data pipelines (mental model)

1. **Ingestion → storage:** IoT device → `ingestion-service` → `energy-usage` → `usage-service` → InfluxDB.
2. **Alerting:** `usage-service` (scheduled aggregation) → threshold exceeded → `energy-alerts` → `alert-service` → SMTP (Mailpit) + MySQL audit.
3. **AI advisory:** client → `insight-service` → `usage-service` (HTTP) → Ollama LLM → natural-language advice.

---

## 3. Repository layout

```
ENERGY_TRACKER
├── AGENTS.md                  # ← this file: global agent/contributor rules
├── README.md                  # Parent/platform master documentation (English)
├── docker-compose.yml         # Unified local infrastructure (MySQL, Redis, Kafka, InfluxDB, Mailpit)
├── postman.json               # Ready-to-import Postman collection (all services)
├── docker/                    # Init scripts & bind-mounted data (mysql/init.sql, influxdb_data/, kafka_data/)
├── img/                       # Architectural schematics (flowchart.jpg)
├── user-service/              # :8080 — README.md + pom.xml + mvnw + src
├── device-service/            # :8081 — README.md + pom.xml + mvnw + src
├── ingestion-service/         # :8082 — README.md + pom.xml + mvnw + src
├── usage-service/             # :8083 — README.md + pom.xml + mvnw + src
├── alert-service/             # :8084 — README.md + pom.xml + mvnw + src
├── insight-service/           # :8085 — README.md + pom.xml + mvnw + src
└── api-gateway/               # :9000 — README.md + pom.xml + mvnw + src
```

> ⚠️ **There is NO root/aggregator `pom.xml`.** Each microservice is an independent Maven
> project with its own `mvnw` wrapper. You cannot build or test the whole platform with a single
> Maven command from the repository root — you must `cd` into each service (or loop over them).
> The parent `README.md` currently shows a root-level `mvn test`; that command is not accurate
> and should be corrected whenever that README is touched.

Every service follows the same internal shape (see §5) and every service has its own
`HELP.md` (Spring Initializr boilerplate — **not** project documentation; ignore it).

---

## 4. Tech stack & versions

Keep new code aligned with the versions already in use. **Do not** introduce a new library,
framework or version unless the user explicitly asks for it.

| Technology | Version | Where it is used |
| :--- | :--- | :--- |
| Java | `21` | All modules (`java.version=21`) |
| Spring Boot | `4.1.1` | Parent POM of every service |
| Spring WebMVC | (SB-managed) | All REST controllers |
| Spring Data JPA / Hibernate | (SB-managed) | user, device, alert |
| Spring Data Redis | (SB-managed) | user, device (Spring Cache) |
| Spring for Apache Kafka | (SB-managed) | ingestion (producer), usage, alert (consumer) |
| Spring Mail | (SB-managed) | alert (SMTP) |
| Spring Cloud | `2025.1.3` | api-gateway only |
| Spring Cloud Gateway (WebMVC) | (SC-managed) | api-gateway (`RouterFunction` routes) |
| Resilience4j Circuit Breaker | (SC-managed) | api-gateway |
| Spring AI / Ollama | `2.0.1` | insight |
| SpringDoc OpenAPI | `3.1.1` (api-gateway: `3.0.2`) | user, device, ingestion, usage, alert, insight, api-gateway |
| Flyway (+ `flyway-mysql`) | (SB-managed) | user, device, alert |
| MySQL | `8` | user, device, alert |
| Redis | `7` | user, device |
| InfluxDB | `2.7` (`influxdb-client-java 6.12.0`) | usage |
| Apache Kafka | KRaft | infrastructure |
| Lombok | `1.18.36` | All modules |
| H2 | (SB-managed, `test`) | JPA slice tests |
| Testcontainers | `1.21.3` | integration tests |
| JUnit 5 / Mockito / AssertJ | (SB-managed) | All modules |
| Maven Wrapper | per service (`mvnw` / `mvnw.cmd`) | Build tool |

> Note: `spring-boot-starter-webmvc` (Spring Boot 4 naming) is used instead of the classic
> `spring-boot-starter-web`. Test slices use the relocated
> `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and
> `org.springframework.test.context.bean.override.mockito.MockitoBean`. Follow these imports —
> do not use the older Spring Boot 3 locations.

---

## 5. Package & layering conventions

Base package pattern: **`com.pgs.<service>.service`** (e.g. `com.pgs.user.service`), except
`api-gateway` → `com.pgs.api.gateway`. Shared Kafka event contracts live in **`com.pgs.kafka.event`**.

Standard layer packages (only create what the service needs):

| Package | Responsibility |
| :--- | :--- |
| `controller` | REST endpoints only. Thin: validate input, delegate to the service, shape the HTTP response. No business logic, no repository access. |
| `service` | Business logic, transaction boundaries (`@Transactional`), DTO↔entity mapping, orchestration. |
| `repository` | Spring Data JPA repositories + `Specification` classes for dynamic filters. |
| `entity` | JPA entities (`@Entity`). `model` is used where a non-JPA domain model is needed. |
| `dto` | API contracts: request/response DTOs, `ErrorResponse`, `PageResponse`. |
| `exception` | Domain exceptions + `GlobalExceptionHandler` (`@RestControllerAdvice`). |
| `config` | Spring `@Configuration` beans (`OpenApiConfig`, `CacheConfig`, `KafkaConfig`, `RestClientConfig`, `InfluxDBConfig`, `OllamaConfig`). |
| `client` | Outbound HTTP clients (`RestClient`-based) to other services. |
| `aspect` | Cross-cutting AOP (`LoggingAspect`, `ExecutionTimeAspect`). |
| `simulation` | Test/load data generators (`ingestion-service` only). |
| `route` | Gateway route definitions (`api-gateway` only). |

**Rules:**
- Follow the layering **strictly**: `Controller → Service → Repository/Client → Store`. Never let a
  controller call a repository or another service's client directly.
- One class per file; one clear responsibility per class (see §6.3).
- Map entities to DTOs **inside the service**, never return entities from a controller.
- Use Lombok consistently: `@RequiredArgsConstructor` + `private final` for injection,
  `@Builder`/`@Data`/`@Value` on DTOs and entities as already done in the module.
- New cross-service HTTP calls: add a `client` class using the existing `RestClient` bean and a
  `config` bean for its base URL property (mirror `usage-service` / `insight-service`).

---

## 6. Mandatory coding rules

These rules are **not optional**. Any new or modified backend code must comply.

### 6.1 JavaDoc on all classes and methods

- Every **class, interface, record and enum** gets a JavaDoc comment describing its purpose.
- Every **method** — public *and* private — gets JavaDoc with `@param` for each parameter,
  `@return` when it returns a value, and `@throws` for each checked/declared exception.
- Entity **fields** get a short one-line JavaDoc (see `User.java`).
- Match the existing style: `/** ... */` blocks, one blank line after the closing tag, English prose.

Example (already the house style):

```java
/**
 * Retrieves a user by ID.
 *
 * @param id the user ID
 * @return the user as a DTO
 * @throws UserNotFoundException if the user does not exist
 */
@Transactional(readOnly = true)
public UserDto getUserById(Long id) { ... }
```

### 6.2 Logging traces

- Use Lombok `@Slf4j` on the class and the `log` field. Never instantiate a logger manually.
- Log **state-changing operations** at `debug` with parameterized messages
  (`log.debug("Created user with id={}", saved.getId());`) — do not use string concatenation.
- Log **entry/exit and timings** through the existing aspects (`LoggingAspect` on the `service`
  package, `ExecutionTimeAspect` on the `controller` package). Reuse them; do not add ad-hoc timing.
- `info` for meaningful lifecycle/business events (message published, alert sent, retry, DLT);
  `warn` for recoverable anomalies; `error` for failures (include the exception as the last arg).
- **Never log secrets, tokens, passwords or PII** (full emails, addresses) at `info` or above.
- Keep aspects' pointcuts in sync with the package they target.

### 6.3 Single Responsibility

- One class = one responsibility. A controller only translates HTTP; a service only orchestrates
  one business capability; a client only talks to one remote service.
- Keep methods small and focused. If a method mixes concerns (validation + persistence + mapping +
  notification), extract private helpers or dedicated collaborators.
- Prefer **composition** of small collaborators over a "god" service class.
- When adding behaviour, ask: *does this class already have a reason to change for this new
  reason?* If yes, create/extend a separate class.

### 6.4 General style

- Match the surrounding file's indentation (some files use tabs, some 4 spaces) and keep lines
  reasonably short.
- Preserve **UTF-8** and Spanish accents in user-facing strings and test data.
- Follow **Conventional Commits** in English: `feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`.
- Do not reformat unrelated code in the same change; keep diffs focused.

---

## 7. Mandatory testing rule

**Every new feature or behaviour change must be covered by tests.** Do not open a change that adds
functionality without accompanying tests. Use the level that matches the risk:

| Level | Annotation / tools | When to use |
| :--- | :--- | :--- |
| **Unit** | `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, AssertJ | Business logic in `service`; collaborators mocked, no Spring context. |
| **Web slice** | `@WebMvcTest(XController.class)` + `@MockitoBean` + `MockMvc` | Controller contracts, validation, status codes and error bodies. |
| **JPA slice** | `@DataJpaTest` + H2 (MySQL mode) | Repositories and `Specification` classes. |
| **Integration / E2E** | `@EmbeddedKafka`, Testcontainers (MySQL/Redis) | Cross-boundary flows (Kafka consume → email → persist, real DBs). |

**Conventions:**
- Test class names end with `Test` (e.g. `UserServiceTest`, `UserControllerTest`).
- Use `@Nested` + `@DisplayName` to group tests per method/behaviour, and factory helpers for test
  data — mirror `UserServiceTest` / `UserControllerTest`.
- Assert with **AssertJ** (`assertThat`, `assertThatThrownBy`), verify interactions with Mockito.
- Cover happy paths **and** error paths (not-found, conflict, validation, empty results).
- `src/test/resources/application.properties` swaps real infra for H2 and disables Flyway when a
  module needs it (see `alert-service`). Add one if your tests need it.
- Run the tests before considering the task done (see §10).

---

## 8. Mandatory documentation rule

**After modifying anything, re-check and update the documentation.** Documentation is part of the
definition of "done".

For **every** change, review **both** of these:

1. **The parent README** — [`README.md`](README.md) at the repository root: platform overview,
   architecture diagram, service table, data pipelines, testing strategy, repository structure.
2. **The microservice README** — `<service>/README.md` for the service(s) you touched.

Update the affected sections whenever your change alters any of:

- endpoints / routes / request-response examples → **API Endpoints**, **Request / Response Examples**;
- configuration properties → **Configuration** table;
- tests added/removed → **Testing** section;
- error handling → **Error Handling** table;
- classes/packages added/removed → **Project Structure** tree;
- dependencies/versions → **Tech Stack** table and the badges at the top;
- a completed roadmap item → tick it in **Roadmap** (and add new ones if you found follow-ups).

Each service README follows the same template — keep it that way:

> Badges → Table of Contents → 🏗️ Architecture (mermaid) → 🧰 Tech Stack → API/Endpoints →
> Request/Response Examples → Data Model → Getting Started → Configuration → 🧪 Testing →
> Error Handling → 📁 Project Structure → 🗺️ Roadmap

If a service has **no** `README.md`, create one from that template (the `api-gateway` gap is now
closed — keep it consistent).

---

## 9. Cross-cutting patterns to reuse

Do not reinvent these; copy the existing implementations.

- **Aspects:** every service has `LoggingAspect` (service layer) and `ExecutionTimeAspect`
  (controller layer). Add to them rather than creating parallel ones.
- **Error contract:** domain exceptions + a `GlobalExceptionHandler` (`@RestControllerAdvice`)
  returning `ErrorResponse { status, error, message }`. Reuse the same JSON shape everywhere.
- **OpenAPI:** `OpenApiConfig` with `@OpenAPIDefinition`; controllers annotated with `@Tag`,
  `@Operation`, `@ApiResponses` (include `ErrorResponse` examples). Register the module's group via
  `springdoc.group-configs` in `application.properties` (note the plural — springdoc silently ignores
  the singular form).
- **Database migrations:** Flyway files under `src/main/resources/db/migration/`, named
  `V<n>__<description>.sql` (e.g. `V1__user_table.sql`). **Never** edit an applied migration; add a
  new version. `spring.jpa.hibernate.ddl-auto=none` — the schema is owned by Flyway.
- **Caching:** Spring Cache with `@Cacheable` / `@CacheEvict` and cache names as constants
  (e.g. `CACHE_USERS = "users"`); see `CacheConfig` in user/device services.
- **Kafka events:** contract classes live in `com.pgs.kafka.event` (`EnergyUsageEvent`,
  `AlertingEvent`). Note the same event is currently **duplicated** per service — when you change an
  event shape, update **every** copy (ingestion, usage, alert) and their READMEs.
- **Kafka producers** are fire-and-forget with a completion callback that only logs; consumers
  delegate immediately to a service and use a DLT for poison messages (`KafkaConfig` in alert).
- **Outbound HTTP:** `RestClient` beans declared in `*Config` classes, base URL injected from
  properties (e.g. `usage.service.url`, `device.service.url`, `user.service.url`).
- **Gateway routing:** one `@Configuration` per service in `api-gateway/route`, each defining the
  route (`RequestPredicates.path(...)` → `uri(...)`) plus a `CircuitBreakerFilterFunctions.circuitBreaker`
  filter and a `/fallbackRoute` returning `503`.

---

## 10. Build, run & verify

**Always verify your change** — at minimum compile + run the affected service's tests.

### Build / test a single service

```bash
cd user-service          # or any other service folder
./mvnw test              # macOS/Linux
.\mvnw.cmd test          # Windows (PowerShell / cmd)
```

To build/test **all** services (there is no root aggregator POM):

```bash
for d in user-service device-service ingestion-service usage-service alert-service insight-service api-gateway; do
  (cd "$d" && ./mvnw -q test) || echo "FAILED: $d"
done
```

### Start local infrastructure

```bash
docker compose up -d     # MySQL, Redis, Kafka (+ Kafka UI), InfluxDB, Mailpit
docker compose down
```

Kafka UI → `http://localhost:8070` · InfluxDB → `http://localhost:8072` · Mailpit UI → `http://localhost:8025`.

### Run a service

```bash
cd <service> && ./mvnw spring-boot:run
```

### Verify after a change

- Service-specific: Swagger UI at `http://localhost:<port>/swagger-ui.html`, spec at `/v3/api-docs`.
- Gateway: `http://localhost:9000/actuator/health` (and `/actuator/circuitbreakers`).
- Exercise endpoints with [`postman.json`](postman.json) (import it into Postman).
- Update `postman.json` when you add or change an endpoint/route.

---

## 11. Do / Don't checklist

**Do**

- ✅ Read this file **and** the target service's `README.md` before changing code.
- ✅ Add JavaDoc to every class/method you add or modify.
- ✅ Add meaningful log traces (`@Slf4j`, parameterized messages) for state changes and failures.
- ✅ Keep one responsibility per class; extract helpers instead of growing a class.
- ✅ Write tests (unit / slice / integration) for every new behaviour and run them.
- ✅ Update the parent `README.md` **and** the affected service `README.md`.
- ✅ Add a new Flyway migration for schema changes; update `postman.json` for API changes.
- ✅ Use Conventional Commits; keep diffs focused on the task.

**Don't**

- ❌ Introduce new dependencies/versions without being asked.
- ❌ Put business logic in controllers or call repositories from controllers.
- ❌ Return JPA entities from controllers (map to DTOs).
- ❌ Edit an already-applied Flyway migration or rely on Hibernate `ddl-auto`.
- ❌ Log secrets/PII, or use string concatenation in log messages.
- ❌ Leave functionality untested or documentation stale.
- ❌ Assume a root `mvn`/`mvnw` build exists — build per service.

---

## 12. Known gaps & technical debt

Useful context so agents don't "rediscover" these; fix opportunistically and update the READMEs.

- **No root/aggregator POM** — build and test per service; the parent README's root `mvn test`
  snippet is misleading and should be corrected.
- **No CI** (`.github/workflows` absent) — no automated build/test gate.
- **Duplicated Kafka event classes** (`EnergyUsageEvent`, `AlertingEvent`) across ingestion, usage
  and alert services; several roadmaps propose extracting a shared module.
- **JavaDoc:** every class, record, enum and method under `src/main/java` now carries JavaDoc
  (verified with an automated scan). Keep it that way — see §6.1.
- **api-gateway** routes `user`, `device`, `ingestion`, `insight` and `usage`, and proxies their five
  OpenAPI specs (`/docs/<service>/v3/api-docs`). **alert-service (:8084)** is intentionally not routed
  or aggregated — it exposes no REST endpoints.
- **usage-service** ships a context-load smoke test plus unit tests for its HTTP clients
  (`DeviceClientTest`, `UserClientTest`); the `UsageService` aggregation/threshold logic still has no
  tests — its README roadmap lists the missing unit/slice/E2E tests.
- **`HELP.md`** files are Spring Initializr boilerplate, not documentation — ignore them (they are
  git-ignored).
- **Java 21 / Spring Boot 4.1.1** naming quirks: `spring-boot-starter-webmvc`,
  `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`,
  `org.springframework.test.context.bean.override.mockito.MockitoBean`.
