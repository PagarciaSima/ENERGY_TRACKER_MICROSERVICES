# 💡 Insight Service

![Build](https://img.shields.io/badge/build-passing-brightgreen) ![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F) ![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-6DB33F) ![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED)

> Java Spring Boot microservice that turns raw energy usage into **actionable, personalized advice** for the **Energy Tracker** home energy tracking platform.

The **Insight Service** is the AI layer of the platform. It pulls the aggregated consumption of a user's devices from the **Usage Service** over HTTP, feeds that data to a local **Ollama** chat model through **Spring AI**, and returns a natural-language answer: either personalized **saving tips** or a concise **overview** of the consumption, always accompanied by the total energy consumed over the analysed period.

It is a **stateless, read-only** service: it owns no database, consumes no Kafka topics and publishes no events. It only orchestrates two collaborators — the Usage Service (data) and the chat model (reasoning) — which keeps the AI layer independently replaceable.

---

## 📑 Table of Contents

- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [API Endpoints](#-api-endpoints)
- [Request / Response Examples](#-request--response-examples)
- [How the Insights are Generated](#-how-the-insights-are-generated)
- [Data Model](#-data-model)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)
- [Actuator & Metrics](#-actuator--metrics)
- [Testing](#-testing)
- [Error Handling](#-error-handling)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)

---

## 🏗️ Architecture

The service follows a classic three-layer flow: a REST controller receives the request, a service orchestrates the data retrieval and the model call, and two aspects add cross-cutting logging and timing.

```mermaid
flowchart LR
    REST[GET /api/v1/insight/overview/:userId] --> Ctrl[InsightController]
    Ctrl --> Svc[InsightService]
    Svc -->|GET /:userId?days=3| Usage[Usage Service :8083]
    Svc -->|Prompt| Model[OllamaChatModel]
    Model --> Ollama[(Ollama :11434)]
    Ctrl -.->|ExecutionTimeAspect| Timer[ExecutionTimeAspect]
    Svc -.->|LoggingAspect| Log[LoggingAspect]
```

- `InsightController` — exposes the two read-only endpoints and documents them with OpenAPI annotations.
- `InsightService` — fetches the usage data, aggregates the total consumption, builds the prompt and wraps the model answer in an `InsightDto`.
- `UsageClient` — `RestClient` based HTTP client against the Usage Service; the base URL is injected from `usage.service.url` through the `usageRestClient` bean.
- `RestClientConfig` — declares the `usageRestClient` bean, built from the auto-configured `RestClient.Builder`, with the Usage Service base URL already applied.
- `OllamaConfig` — declares the `ChatClient` bean with the default system prompt that turns the model into an *expert energy efficiency advisor*.
- `GlobalExceptionHandler` — maps exceptions to a single, stable error contract.

> The chat model is invoked through the `OllamaChatModel` bean (Spring AI) rather than through the `ChatClient` bean, so the per-request prompt is the only thing sent on each call.

---

## 🧰 Tech Stack

| Technology                 | Version    | Purpose                                        |
| -------------------------- | ---------- | ---------------------------------------------- |
| Java                       | 21         | Language / runtime                             |
| Spring Boot                | 4.1.1      | Application framework                          |
| Spring WebMVC              | (SB)       | REST controller                                |
| Spring AI                  | 2.0.1      | `ChatClient` / `OllamaChatModel` abstraction   |
| Ollama                     | —          | Local LLM runtime (model `llama3.2`)           |
| RestClient                | (SB)       | HTTP client to the Usage Service               |
| AspectJ (Spring AOP)       | (SB)       | Aspect-oriented logging / timing               |
| Spring Boot Actuator       | (SB)       | Health, info and metrics endpoints             |
| Micrometer Prometheus Registry | (SB)   | Metrics in Prometheus exposition format        |
| jackson-datatype-jsr310    | —          | `Instant` / time API serialization             |
| SpringDoc OpenAPI          | 3.1.1      | Swagger UI / API documentation                 |
| Lombok                     | 1.18.36    | Boilerplate reduction                          |
| Maven                      | —          | Build tool                                     |
| Docker                     | —          | Local infra (MySQL, Redis, Kafka, InfluxDB)   |
| Actuator + Prometheus      | —          | Health / metrics endpoint                      |
| JUnit 5 / Mockito / AssertJ | —        | Testing                                        |

---

## 🔗 API Endpoints

Both endpoints live under the base path `/api/v1/insight`.

> Interactive docs available at [Swagger UI](http://localhost:8085/swagger-ui.html) (OpenAPI spec at [`/v3/api-docs`](http://localhost:8085/v3/api-docs)).

| Method | Path                    | Description                                                  | HTTP statuses       |
| ------ | ----------------------- | ------------------------------------------------------------ | ------------------- |
| `GET`  | `/saving-tips/{userId}` | Personalized tips to reduce consumption + total energy usage | `200`, `400`, `500` |
| `GET`  | `/overview/{userId}`    | Concise analysis of the consumption + total energy usage      | `200`, `400`, `500` |

Path parameters:

- `userId` (path): the user id; must be a valid `Long` (anything else yields `400`).

There are **no** query parameters: the analysed window is fixed by the service to the **last 3 days**.

---

## 📥 Request / Response Examples

### Get the saving tips for user 1

```bash
curl http://localhost:8085/api/v1/insight/saving-tips/1
```

**Response `200 OK`**

```json
{
  "userId": 1,
  "tips": "Consider shifting heavy loads to off-peak hours and unplugging idle devices.",
  "energyUsage": 12.75
}
```

![Saving tips through the gateway](../img/get_saving_tips_ollama_endpoint.png)
*The same endpoint called through the gateway with a Bearer token: `200 OK` in ~28 s, returning the `llama3.2` tips and the aggregated `energyUsage` of the last 3 days.*

### Get the consumption overview for user 1

```bash
curl http://localhost:8085/api/v1/insight/overview/1
```

**Response `200 OK`**

```json
{
  "userId": 1,
  "tips": "Your consumption is in line with similar households. The HVAC system accounts for most of the load; consider a smart thermostat.",
  "energyUsage": 12.75
}
```

**Response `400 Bad Request`** if the `userId` is not a number:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Parameter 'userId' must be of type 'Long'"
}
```

**Response `500 Internal Server Error`** if the Usage Service or the chat model is unreachable:

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Unexpected error"
}
```

---

## 🧠 How the Insights are Generated

Both endpoints follow the **same three-step pipeline**, differing only in the prompt that is sent to the model:

1. **Fetch** — `UsageClient.getXDaysUsageForUser(userId, 3)` calls the Usage Service:
   `GET {usage.service.url}/{userId}?days=3`.
2. **Aggregate** — the total consumption is `sum(device.energyConsumed)` over the returned devices. A user with no devices yields `0.0`.
3. **Ask** — a prompt is built and sent to the Ollama chat model; the text of the answer becomes the `tips` field of the response.

The two prompts are different on purpose:

| Endpoint              | Prompt content                                                                 | Goal                                                 |
| --------------------- | ----------------------------------------------------------------------------- | ----------------------------------------------------- |
| `/saving-tips/{id}`   | The **total** consumption only (`4.75`)                                        | *"How can I reduce my energy consumption? How does it compare to average households?"* |
| `/overview/{id}`      | The **full device list** (name, type, location, consumption of each device)    | *"Analyse the data and provide a concise overview with actionable insights."*  |

The chat model is called through the `OllamaChatModel` bean (Spring AI) with a per-request `Prompt`. The `ChatClient` bean declared in `OllamaConfig` carries the **default system prompt** that frames the model as an *expert energy efficiency advisor* and asks it for concise, practical advice.

> The service is **synchronous**: the HTTP request blocks until the model answers. There is no streaming, no queue and no retry.

---

## 🗄️ Data Model

This service has **no database**. Its data lives in two places: the Usage Service (aggregated consumption) and the chat model (the generated text).

### Response DTOs

| DTO            | Fields                                    | Description                                          |
| -------------- | ----------------------------------------- | ---------------------------------------------------- |
| `InsightDto`   | `userId`, `tips`, `energyUsage`           | The generated insight and the total consumption (kWh) |
| `UsageDto`     | `userId`, `devices`                       | Usage data as returned by the Usage Service           |
| `DeviceDto`    | `id`, `name`, `type`, `location`, `energyConsumed` | A device and its aggregated consumption      |
| `ErrorResponse`| `status`, `error`, `message`              | Standard error body for 4xx / 5xx responses           |

### Upstream contract (Usage Service)

The client expects the Usage Service to answer with:

```json
{
  "userId": 1,
  "devices": [
    { "id": 42, "name": "Smart Bulb Salon", "type": "LIGHT", "location": "Salon principal", "energyConsumed": 3.5 },
    { "id": 43, "name": "Frigorifico", "type": "FRIDGE", "location": "Cocina", "energyConsumed": 1.25 }
  ]
}
```

---

## 🚀 Getting Started

### Prerequisites

The service depends on two collaborators:

| Dependency       | Where                      | Required for                                       |
| ---------------- | -------------------------- | -------------------------------------------------- |
| **Usage Service** | `http://localhost:8083`   | The aggregated consumption data                     |
| **Ollama**        | `http://localhost:11434`  | The chat model (`llama3.2` must be available) |

The **Usage Service** (`:8083`) expects **Kafka** and **InfluxDB** to be reachable, so the platform infrastructure (MySQL, Redis, Kafka, InfluxDB) is started from the repository root with Docker Compose:

```bash
docker compose up -d
```

The Usage Service itself is a separate Spring Boot application, so it must be started as well (from the `usage-service` folder):

```bash
mvn spring-boot:run
```

> **Ollama is not part of the bundled `docker-compose.yml`.** It runs on the host (or on any reachable machine) with the model already pulled. The base URL is configurable through `spring.ai.ollama.base-url` / `SPRING_AI_OLLAMA_BASE_URL`.

```bash
# install Ollama from https://ollama.com, then:
ollama serve
ollama pull llama3.2         # must match spring.ai.ollama.chat.options.model
```

Verify that the server is actually listening **before** starting this service:

```bash
curl http://localhost:11434/api/tags      # expected: {"models":[...]}
ollama list                               # expected: llama3.2:latest (the configured model)
```

> 🐳 When this service runs as a container, Compose points it at the host with `SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434`.

> 🩺 **Troubleshooting (Windows):** the desktop app (`ollama app.exe`) can stay alive while its HTTP server is dead. In that state `ollama list` answers `existing instance found… Error: timed out waiting for server to start` and `%LOCALAPPDATA%\Ollama\app.log` shows `ollama server not ready after retries`. Fix it by killing the zombie and starting the server again:
>
> ```powershell
> taskkill /F /IM "ollama app.exe"
> ollama serve
> ```
>
> Without a listening server every insight call ends in a `503` (`Ollama is unavailable: Connection refused`).

### Running locally

1. Start the infrastructure, the Usage Service and Ollama (see above).
2. Run this service:

   ```bash
   mvn spring-boot:run
   ```

3. The service is available at `http://localhost:8085` (Swagger UI at `http://localhost:8085/swagger-ui.html`).

### Running as a container

This service ships a `Dockerfile` (multi-stage build, non-root user, `/actuator/health` health check) and belongs to the Compose `app` profile:

```bash
# from the repository root: builds and starts the whole platform
docker compose --env-file .env.docker up -d --build

# or just this service (the infrastructure must already be running)
docker compose --env-file .env.docker up -d --build insight-service
```

| Environment variable | Default (host run) | Value injected by Compose |
| :--- | :--- | :--- |
| `USAGE_SERVICE_URL` | `http://localhost:8083/api/v1/usage` | `http://usage-service:8083/api/v1/usage` |
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | `http://host.docker.internal:11434` |

> Ollama is **not** part of the Compose stack: the container reaches the host instance through `host.docker.internal` (see `extra_hosts` in `docker-compose.yml`). If Ollama is down, the endpoint answers `503` in ~1-3 s instead of hanging.

> The consumption data is produced by the **Ingestion Service** (`:8082`) → Kafka → **Usage Service** (`:8083`). You can use the bundled data simulators there to generate traffic.

---


## ⚙️ Configuration

| Property                                | Default                                | Purpose                                       |
| --------------------------------------- | -------------------------------------- | --------------------------------------------- |
| `server.port`                           | `8085`                                 | HTTP port                                     |
| `usage.service.url`                     | `http://localhost:8083/api/v1/usage`   | Usage Service base URL                        |
| `spring.ai.ollama.chat.options.model`   | `llama3.2`                             | Chat model used to generate the insights      |
| `spring.ai.ollama.base-url`             | `http://localhost:11434`               | Ollama server address (Spring AI default)     |
| `spring.ai.ollama.init.pull-model-strategy` | `never`                             | Do not pull the model on startup              |
| `spring.ai.retry.max-attempts`          | `2`                                    | Attempts per model call (Spring AI default: `10`) |
| `spring.ai.retry.backoff.initial-interval` | `1s`                                | Initial wait between attempts (default: `2000ms`) |
| `spring.ai.retry.backoff.multiplier`    | `2`                                    | Backoff multiplier (default: `5`)             |
| `spring.ai.retry.backoff.max-interval`  | `2s`                                   | Cap for the backoff (no default: unbounded)   |
| `management.endpoints.web.exposure.include` | `health,info,prometheus`           | Actuator endpoints exposed                    |
| `management.endpoint.health.show-details`   | `when-authorized`                  | Health detail visibility                      |
| `management.metrics.tags.application`       | `insight-service`                  | Common tag added to all metrics               |
| `springdoc.swagger-ui.path`             | `/swagger-ui.html`                     | Swagger UI path                               |
| `springdoc.group-configs[0].group`       | `insight`                              | OpenAPI group for the insight endpoints       |

> `spring.ai.ollama.init.pull-model-strategy=never` means the service **will not** download the model: it must already be present in Ollama, otherwise every call fails with a `500`.

> `usage.service.url` is only read when the `usageRestClient` bean is created. Since it is bound to the `RestClient.Builder` auto-configured by Spring Boot, any application-wide customizer (message converters, observability, SSL) is applied to the outbound calls as well.

---

## 📊 Actuator & Metrics

The service ships **Spring Boot Actuator** and the **Micrometer Prometheus registry**, so its runtime metrics are exposed in Prometheus exposition format and scraped every 15 seconds by the `prometheus` container declared in [`docker-compose.yml`](../docker-compose.yml) (job `insight-service` in [`docker/prometheus/prometheus.yml`](../docker/prometheus/prometheus.yml)).

| Endpoint               | Purpose                                                 |
| ---------------------- | ------------------------------------------------------- |
| `/actuator/health`     | Liveness / readiness probe                              |
| `/actuator/info`       | Application & build information                         |
| `/actuator/prometheus` | Metrics in Prometheus exposition format (scrape target) |

```bash
curl http://localhost:8085/actuator/health
curl http://localhost:8085/actuator/prometheus
```

Every meter is tagged with `application=insight-service` (`management.metrics.tags.application`), which makes per-service filtering trivial in Grafana — reachable at [http://localhost:3000](http://localhost:3000) (`admin` / `admin`) with the Prometheus datasource already provisioned.

---

## 🧪 Testing

```bash
cd insight-service
./mvnw test
```

The suite runs **without any external infrastructure** (no Docker, no Usage Service and no Ollama instance required):

| Test                              | Level       | What it verifies                                                            |
| --------------------------------- | ----------- | --------------------------------------------------------------------------- |
| `UsageClientTest`                 | Unit        | URL/params of the Usage Service call, JSON deserialization, 404 / 500 mapping |
| `InsightServiceTest`              | Unit        | Energy aggregation, prompt content, failure propagation, translation of Usage Service / Ollama failures into `DependencyUnavailableException` |
| `InsightControllerTest`           | Web slice   | `200` payloads, `400` on a non-numeric `userId`, `500` and `503` contracts  |
| `GlobalExceptionHandlerTest`      | Unit        | Error bodies of validation, type mismatch, dependency unavailable (`503`) and generic exceptions |
| `ExecutionTimeAspectTest`         | Unit        | Timing advice returns the result and propagates exceptions                  |
| `LoggingAspectTest`               | Unit        | `@Before` / `@AfterReturning` advice behaviour                              |
| `OllamaConfigTest`                | Unit        | The `ChatClient` system prompt is configured                                |
| `InsightApiIntegrationTest`       | Integration | Full HTTP flow over a random port with a mocked chat model and usage client |

- The `UsageClient` HTTP calls are stubbed with `MockRestServiceServer`, bound to the `RestClient.Builder` (`MockRestServiceServer.bindTo(builder)`).
- The chat model is mocked with `ChatResponse` / `Generation` / `AssistantMessage` (Spring AI types).
- Spring Boot 4 test slices use `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` and `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- `InsightServiceApplicationTests` boots the full Spring context to verify the application wiring.

Current status: **38 tests, 0 failures**.

---

## 🛑 Error Handling

Errors are centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`). All error responses share a common `ErrorResponse` body: `{ "status": ..., "error": ..., "message": ... }`.

| Exception                          | HTTP status | Reason                                             |
| ---------------------------------- | ----------- | -------------------------------------------------- |
| `MethodArgumentNotValidException`  | `400`       | Request body validation failed                     |
| `MethodArgumentTypeMismatchException` | `400`    | `userId` is not a valid `Long`                     |
| `DependencyUnavailableException`   | `503`       | The Usage Service or the chat model (Ollama) is not reachable |
| `Exception` (generic fallback)     | `500`       | Unexpected error                                   |

> Failures coming from the **Usage Service** or from the **chat model** (`RestClientException` and its subclasses: connection refused, timeouts, `5xx`) are translated into a `DependencyUnavailableException` by `InsightService` and answered as **`503 Service Unavailable`**, naming the unreachable dependency, for example:
>
> ```json
> { "status": 503, "error": "Service Unavailable", "message": "Ollama is unavailable: Connection refused" }
> ```
>
> ⏱️ Thanks to the retry configuration (`spring.ai.retry.*`, see [Configuration](#-configuration)) a dead Ollama is reported in **~1-2 seconds**. With the Spring AI defaults (10 attempts, exponential backoff of `2s × 5` with no cap: 2s, 10s, 50s, 250s…) the HTTP request **never completes**, so the client hangs instead of receiving an error.

---

## 📁 Project Structure

```
insight-service
├── Dockerfile                 # Multi-stage build: maven:3.9 + JRE 21, non-root user, /actuator/health healthcheck
├── .dockerignore              # Keeps target/, doc/ and the IDE files out of the build context
├── mvnw / mvnw.cmd            # Maven wrapper (./mvnw test on macOS/Linux, .\mvnw.cmd test on Windows)
├── .mvn/wrapper/maven-wrapper.properties
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/pgs/insight/service
│   │   │   ├── InsightServiceApplication.java
│   │   │   ├── aspect
│   │   │   │   ├── ExecutionTimeAspect.java
│   │   │   │   └── LoggingAspect.java
│   │   │   ├── client
│   │   │   │   └── UsageClient.java
│   │   │   ├── config
│   │   │   │   ├── OllamaConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── controller
│   │   │   │   └── InsightController.java
│   │   │   ├── dto
│   │   │   │   ├── DeviceDto.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── InsightDto.java
│   │   │   │   └── UsageDto.java
│   │   │   ├── exception
│   │   │   │   ├── DependencyUnavailableException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── service
│   │   │       └── InsightService.java
│   │   └── resources
│   │       └── application.properties
│   └── test
│       └── java/com/pgs/insight/service
│           ├── InsightServiceApplicationTests.java
│           ├── aspect
│           │   ├── ExecutionTimeAspectTest.java
│           │   └── LoggingAspectTest.java
│           ├── client
│           │   └── UsageClientTest.java
│           ├── config
│           │   └── OllamaConfigTest.java
│           ├── controller
│           │   └── InsightControllerTest.java
│           ├── exception
│           │   └── GlobalExceptionHandlerTest.java
│           ├── integration
│           │   └── InsightApiIntegrationTest.java
│           └── service
│               └── InsightServiceTest.java
```

---

## 🗺️ Roadmap

- [ ] Add an Ollama service to the bundled `docker-compose.yml` so the whole platform can be started with a single command.
- [ ] Make the analysed window configurable (`?days=` query parameter) instead of the hardcoded 3 days.
- [ ] Stream the model answer with SSE so long generations are not blocked.
- [x] Add a timeout / retry / circuit breaker around the `UsageClient` and the chat model calls — the chat model path now fails fast (`spring.ai.retry.*`: 2 attempts, 1s backoff capped at 2s) and answers `503 Service Unavailable`.
- [ ] Add an explicit connect/read timeout to the `usageRestClient` (`ClientHttpRequestFactory`) so a hung Usage Service cannot block a request either.
- [ ] Persist the generated insights so they can be listed later and compared over time.
- [ ] Expose the model and the system prompt as configurable properties.
- [ ] Add an integration test against a real Ollama instance via Testcontainers.
