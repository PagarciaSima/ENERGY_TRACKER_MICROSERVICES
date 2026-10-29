# 🧪 plan.md — Guion de pruebas manuales E2E y capturas de pantalla

> **Objetivo de este documento:** recorrer la plataforma **Home Energy Tracker** de punta a punta
> desde **Postman**, en orden, verificando cada capa (REST → Kafka → InfluxDB → MySQL → SMTP →
> Ollama → Observabilidad) y **tomando capturas de pantalla** en los puntos marcados con 📸.
> Las capturas se integrarán en los READMEs (raíz + cada microservicio) en una **tarea posterior**;
> este plan **no** modifica documentación, solo la guía de pruebas y las imágenes.
>
> **Estado:** planificado (checklist). Marca cada paso con `[x]` a medida que lo completes.

---

## 📑 Índice

- [0. Convenciones y estructura de capturas](#0-convenciones-y-estructura-de-capturas)
- [1. Estado del entorno verificado](#1-estado-del-entorno-verificado)
- [2. Referencia rápida (puertos, URLs, credenciales)](#2-referencia-rápida-puertos-urls-credenciales)
- [3. Fase 0 — Preparación previa](#3-fase-0--preparación-previa)
- [4. Fase 1 — Arranque ordenado de microservicios](#4-fase-1--arranque-ordenado-de-microservicios)
- [5. Fase 2 — User Service (:8080)](#5-fase-2--user-service-8080)
- [6. Fase 3 — Device Service (:8081)](#6-fase-3--device-service-8081)
- [7. Fase 4 — Keycloak + API Gateway (:9000)](#7-fase-4--keycloak--api-gateway-9000)
- [8. Fase 5 — Ingestion → Kafka](#8-fase-5--ingestion--kafka)
- [9. Fase 6 — InfluxDB](#9-fase-6--influxdb)
- [10. Fase 7 — Usage Service (:8083)](#10-fase-7--usage-service-8083)
- [11. Fase 8 — Alerting E2E (Kafka → Mailpit → MySQL)](#11-fase-8--alerting-e2e-kafka--mailpit--mysql)
- [12. Fase 9 — Insight Service + Ollama (:8085)](#12-fase-9--insight-service--ollama-8085)
- [13. Fase 10 — Observabilidad (Prometheus / Grafana / Actuator)](#13-fase-10--observabilidad-prometheus--grafana--actuator)
- [14. Fase 11 — Resiliencia: circuit breaker del gateway](#14-fase-11--resiliencia-circuit-breaker-del-gateway)
- [15. Fase 12 — Errores y validaciones](#15-fase-12--errores-y-validaciones)
- [16. Fase 13 — Tests automáticos por servicio](#16-fase-13--tests-automáticos-por-servicio)
- [17. Fase 14 — Limpieza y repetición de la demo](#17-fase-14--limpieza-y-repetición-de-la-demo)
- [18. Checklist maestro de capturas](#18-checklist-maestro-de-capturas)
- [19. Cambios de código opcionales (revertir después)](#19-cambios-de-código-opcionales-revertir-después)
- [20. Limitaciones conocidas y notas](#20-limitaciones-conocidas-y-notas)
- [21. Siguiente tarea: integrar las capturas en los READMEs](#21-siguiente-tarea-integrar-las-capturas-en-los-readmes)
- [22. Bugs detectados durante las pruebas](#22-bugs-detectados-durante-las-pruebas)
- [23. Bug: la petición de insight se cuelga cuando Ollama está caído](#23-bug-la-petición-de-insight-se-cuelga-cuando-ollama-está-caído)
- [24. Bugs del gateway detectados al verificar insight](#24-bugs-del-gateway-detectados-al-verificar-insight)

---

## 0. Convenciones y estructura de capturas

**Carpeta de capturas:** [`img/screenshots/`](img/screenshots/)

**Convención de nombre:** `fase<NN>-<nn>-<tema>.png`

| Parte | Significado | Ejemplo |
| :--- | :--- | :--- |
| `fase<NN>` | número de fase de este plan (2 dígitos) | `fase07` |
| `<nn>` | orden dentro de la fase (2 dígitos) | `03` |
| `<tema>` | descripción corta en kebab-case | `mailpit-alerta` |

Ejemplo completo: `img/screenshots/fase08-03-mailpit-alerta.png`

**Reglas de captura:**

- ✅ Resolución completa de la ventana (1080p o superior), sin recortes que oculten la URL/puerto.
- ✅ En Postman, mostrar siempre **método + URL + status + tiempo + body** (vista *Pretty*).
- ✅ En Swagger UI, mostrar el endpoint desplegado con su respuesta.
- ❌ **No capturar tokens, `client_secret`, ni datos personales reales.** El `client_secret` de
  desarrollo (`KHe2IG9JY8uWoSqi8l2gbm5GOLXkVewm`) y el `access_token` deben quedar **fuera de plano**;
  si aparecen, difumínalos antes de subirlos al repo.
- ✅ Usa datos de prueba ficticios (`User1`, `demo.alert@example.com`, etc.).
- ✅ Tema claro/oscuro consistente entre capturas de la misma herramienta.

---

## 1. Estado del entorno verificado

> Comprobado el **29/10/2026** con el stack Docker arriba y los microservicios **parados**.

| Comprobación | Resultado | Implicación para el plan |
| :--- | :--- | :--- |
| Contenedores Docker | ✅ `mysql`, `user-service-redis`, `kafka`, `kafka-ui`, `influxdb`, `mailpit`, `prometheus`, `grafana`, `keycloak` | Infra lista; solo faltan los micros |
| Micros en host (`8080`-`8085`, `9000`) | ❌ Ninguno responde a `/actuator/health` | Empezar por la Fase 1 |
| MySQL (contenedor `mysql`, root/admin, `127.0.0.1:3306`) | ✅ Ya existen `home_energy_tracker_user`, `home_energy_tracker_device` y `home_energy_tracker_alert` con sus tablas Flyway (`user`, `device`, `alert`) + `flyway_schema_history`. **La BD `home_energy_tracker` NO existe** | **Flyway crea las BDs de servicio automáticamente**: no hace falta crearlas a mano. Si en tu cliente MySQL ves `home_energy_tracker` + `sakila` + `test`, estás conectado a **otra instancia** (una instalación local), no al contenedor |
| Datos previos en MySQL | 10 users (`User1`..`User10`), 200 devices, 259 alerts | Hay datos de pruebas anteriores: ver [Fase 14](#17-fase-14--limpieza-y-repetición-de-la-demo) si quieres partir de cero |
| Kafka topics | `energy-usage`, `energy-alerts`, `__consumer_offsets` | El DLT `energy-alerts-dlt` se autocrea cuando se usa (ver [limitaciones](#20-limitaciones-conocidas-y-notas)) |
| InfluxDB | bucket `usage-bucket` (org `home-energy-tracker`) **VACÍO**: 0 measurements | **Sin ingerir datos no habrá nada que capturar** en Influx ni en el dashboard de Grafana |
| Mailpit | 0 mensajes | Limpio para la demo de alertas |
| Grafana | Dashboard `Home Energy Tracker - Overview` (`uid: het-overview`) ya provisionado; datasource Prometheus con `uid` fijo | Captura directa, sin configuración manual |
| Keycloak | Realm `het-security-realm`, client `home-energy-tracker-client` **confidencial** con `serviceAccountsEnabled=true` y **`directAccessGrantsEnabled=false`**, **0 usuarios finales** | El *client credentials* funciona ya; el *password grant* fallará (`unauthorized_client`) hasta habilitar Direct Access Grants y crear un usuario → [Fase 0](#3-fase-0--preparación-previa) |
| Ollama | Binario instalado, modelo `deepseek-coder:latest` descargado, **servidor parado** (11434 no responde) | Arrancar Ollama antes de la Fase 9 |

Comandos de verificación rápida (solo lectura):

```bash
docker ps --format '{{.Names}} | {{.Status}} | {{.Ports}}'
docker exec mysql mysql -uroot -padmin -e "show databases;"
docker exec kafka bash -c "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list"
curl -s -H "Authorization: Token my-token" "http://localhost:8072/api/v2/buckets?org=home-energy-tracker"
curl -s http://localhost:8025/api/v1/messages
```

---

## 2. Referencia rápida (puertos, URLs, credenciales)

| Recurso | URL / Puerto | Credenciales |
| :--- | :--- | :--- |
| User Service | `http://localhost:8080` (Swagger `/swagger-ui.html`) | — |
| Device Service | `http://localhost:8081` (Swagger `/swagger-ui.html`) | — |
| Ingestion Service | `http://localhost:8082` (Swagger `/swagger-ui.html`) | — |
| Usage Service | `http://localhost:8083` (Swagger `/swagger-ui.html`) | — |
| Alert Service | `http://localhost:8084` (**sin REST**; solo `/actuator/*`) | — |
| Insight Service | `http://localhost:8085` (Swagger `/swagger-ui.html`) | — |
| API Gateway | `http://localhost:9000` (Swagger `/swagger-ui.html`, specs en `/docs/<service>/v3/api-docs`) | 🔒 Bearer JWT |
| MySQL | `localhost:3306` | `root` / `admin` |
| Redis | `localhost:6379` | — |
| Kafka (host) | `localhost:9094` (interno `kafka:9092`) | — |
| Kafka UI (Kafbat) | `http://localhost:8070` | — |
| InfluxDB | `http://localhost:8072` | `admin` / `admin123`, org `home-energy-tracker`, bucket `usage-bucket`, token `my-token` |
| Mailpit (UI) | `http://localhost:8025` (SMTP `1025`) | — |
| Prometheus | `http://localhost:9090` | — |
| Grafana | `http://localhost:3000` | `admin` / `admin` |
| Keycloak | `http://localhost:8091` | `admin` / `admin`, realm `het-security-realm`, client `home-energy-tracker-client` |

**Endpoints reales por servicio** (verificados en el código):

| Servicio | Método y ruta | Notas |
| :--- | :--- | :--- |
| user | `POST /api/v1/user` | crear usuario |
| user | `GET /api/v1/user` · `GET /api/v1/user/page?page&size` | listado completo / paginado |
| user | `GET /api/v1/user/search?name&surname&email&address&alerting&minEnergyAlertingThreshold&page&size` | filtros dinámicos (Specification) |
| user | `GET /api/v1/user/{id}` · `PUT /api/v1/user/{id}` · `DELETE /api/v1/user/{id}` | CRUD |
| user | `PATCH /api/v1/user/{id}/alerting?enabled=` (`true` o `false`) | toggle de alertas |
| device | `POST /api/v1/device` · `GET /api/v1/device?page&size` | crear / paginar |
| device | `GET /api/v1/device/search?name&type&location&userId&page&size` | filtros |
| device | `GET /api/v1/device/{id}` · `PUT /api/v1/device/{id}` · `DELETE /api/v1/device/{id}` | CRUD |
| device | `GET /api/v1/device/user/{userId}?page&size` | devices de un usuario |
| ingestion | `POST /api/v1/ingestion` | `{deviceId, energyConsumed, timestamp}` → Kafka `energy-usage` |
| usage | `GET /api/v1/usage/{userId}?days=3` | agregado por device (1-365 días) |
| insight | `GET /api/v1/insight/saving-tips/{userId}` · `GET /api/v1/insight/overview/{userId}` | Ollama |
| alert | *(sin endpoints REST)* | consume `energy-alerts`, envía email, audita en MySQL |

**Tipos de device válidos** (`DeviceType`): `SPEAKER`, `CAMERA`, `THERMOSTAT`, `LIGHT`, `LOCK`, `DOORBELL`.

**Variables de la colección Postman** (`postman.json`): `gatewayBaseUrl`, `keycloakBaseUrl`, `realm`,
`clientId`, `clientSecret`, `username` (`test-user`), `password` (`test-password`), `accessToken`,
`refreshToken`, `userBaseUrl`, `deviceBaseUrl`, `ingestionBaseUrl`, `usageBaseUrl`, `insightBaseUrl`,
`userId` (`1`), `deviceId` (`1`). La colección tiene **auth Bearer heredada** con `{{accessToken}}`.

---

## 3. Fase 0 — Preparación previa

| # | Paso | Comando / acción | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 0.1 | Infra arriba | `docker compose up -d` y `docker ps` | `fase00-01-docker-ps.png` |
| 0.2 | Comprobar BDs reales | MySQL Workbench → conexión a `127.0.0.1:3306` (root/admin) → deben aparecer `home_energy_tracker_user`, `_device`, `_alert` | `fase00-02-mysql-schemas.png` |
| 0.3 | Arrancar Ollama | `ollama serve` (o la app de escritorio) y `curl http://localhost:11434/api/tags` | `fase00-03-ollama-tags.png` |
| 0.4 | Keycloak: habilitar Direct Access Grants | `http://localhost:8091` (admin/admin) → realm `het-security-realm` → *Clients* → `home-energy-tracker-client` → *Capability config* → activar **Direct access grants** → *Save* | `fase00-04-keycloak-client-dag.png` |
| 0.5 | Keycloak: crear usuario de pruebas | *Users* → *Add user* → username `test-user`, email `test-user@example.com` → *Credentials* → *Set password* `test-password` (**Temporary: Off**) | `fase00-05-keycloak-user.png` |
| 0.6 | Importar la colección | Postman → *Import* → `postman.json`; revisar *Variables* (`userId=1`, `deviceId=1`) | `fase00-06-postman-variables.png` |
| 0.7 | *(Opcional)* Partir de cero | Ver [Fase 14](#17-fase-14--limpieza-y-repetición-de-la-demo) | — |

> ⚠️ **Sobre `directAccessGrantsEnabled=false`:** tal cual está el realm exportado, la request
> *Get access token (password grant)* devuelve `400 unauthorized_client`. Dos caminos:
> **(a)** activar *Direct access grants* + crear el usuario (recomendado: deja el realm usable y
> permite capturar el flujo password grant), o **(b)** usar solo *client credentials*, que ya
> funciona porque el client tiene `serviceAccountsEnabled=true`. El gateway valida **solo la firma
> contra el JWKS** (`keycloak.auth.jwk-set-uri`), así que ambos tokens sirven para rutear.
> Si haces (a), **re-exporta el realm** a `docker/keycloak/realms/` para versionarlo.

---

## 4. Fase 1 — Arranque ordenado de microservicios

Orden recomendado (usage necesita user+device; insight necesita usage; gateway necesita todos).

```bash
cd user-service     && .\mvnw.cmd spring-boot:run   # :8080
cd device-service   && .\mvnw.cmd spring-boot:run   # :8081
cd usage-service    && .\mvnw.cmd spring-boot:run   # :8083
cd alert-service    && .\mvnw.cmd spring-boot:run   # :8084
cd ingestion-service && .\mvnw.cmd spring-boot:run  # :8082
cd insight-service  && .\mvnw.cmd spring-boot:run   # :8085
cd api-gateway      && .\mvnw.cmd spring-boot:run   # :9000
```

| # | Verificación | 📸 Captura |
| :-- | :--- | :--- |
| 1.1 | Log de arranque de **user-service** (`Flyway ... Successfully applied`, `Tomcat started on port 8080`, `Started UserServiceApplication`) | `fase01-01-user-logs.png` |
| 1.2 | Log de arranque de **device-service** (Flyway + puerto 8081) | `fase01-02-device-logs.png` |
| 1.3 | Log de arranque de **usage-service** (`@KafkaListener` iniciado + cliente InfluxDB) | `fase01-03-usage-logs.png` |
| 1.4 | Log de arranque de **alert-service** (Kafka consumer + JavaMailSender) | `fase01-04-alert-logs.png` |
| 1.5 | Log de arranque de **ingestion-service** (producer Kafka + `ContinuousDataSimulator started...`) | `fase01-05-ingestion-logs.png` |
| 1.6 | Log de arranque de **insight-service** (Spring AI / Ollama chat model) | `fase01-06-insight-logs.png` |
| 1.7 | Log de arranque de **api-gateway** (rutas + JWT decoder contra el JWKS de Keycloak) | `fase01-07-gateway-logs.png` |
| 1.8 | Health de los 7: `http://localhost:<puerto>/actuator/health` | `fase01-08-actuator-health.png` |
| 1.9 | Kafka UI → *Consumers*: grupos `usage-service` y `alert-service` registrados | `fase01-09-kafka-consumers.png` |

Comprobación en bloque (PowerShell):

```powershell
foreach ($p in 8080,8081,8082,8083,8084,8085,9000) { "$p -> " + (Invoke-WebRequest "http://localhost:$p/actuator/health" -UseBasicParsing).Content }
```

---

## 5. Fase 2 — User Service (:8080)

| # | Request Postman (carpeta *User Service*) | Resultado esperado | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 2.1 | Swagger UI `http://localhost:8080/swagger-ui.html` | Grupo **users** con las 8 operaciones | `fase02-01-swagger-user.png` |
| 2.2 | *Create user* (`POST /api/v1/user`) | `201 Created` + `Location` + body con `id` | `fase02-02-create-user.png` |
| 2.3 | *List all users* (`GET /api/v1/user`) | `200` + array de `UserDto` | `fase02-03-list-users.png` |
| 2.4 | *List users (paginated)* (`GET /api/v1/user/page?page=0&size=10`) | `200` + `PageResponse` (content, totalElements, totalPages…) | `fase02-04-page-users.png` |
| 2.5 | *Search users* | `200` con filtros aplicados. ⚠️ El ejemplo del collection usa `minEnergyAlertingThreshold=2500`; con los datos actuales (umbrales 1001-1010) **devuelve vacío** → baja el valor (p. ej. `1005`) o crea usuarios con umbral alto antes de capturar | `fase02-05-search-users.png` |
| 2.6 | *Get user by id* (`GET /api/v1/user/{{userId}}`) | `200` con el usuario | — |
| 2.7 | *Update user* (`PUT /api/v1/user/{{userId}}`) | `200` con los datos nuevos | `fase02-06-update-user.png` |
| 2.8 | *Enable/disable alerting* (`PATCH /api/v1/user/{{userId}}/alerting?enabled=false`) | `200` y `alerting=false` | `fase02-07-toggle-alerting.png` |
| 2.9 | Repetir *Get user by id* **2 veces** y observar el log `TRACE org.springframework.cache` (2ª llamada sin `SELECT`) | Caché Redis (`@Cacheable` cache `users`) | `fase02-08-redis-cache.png` |
| 2.10 | Redis: `docker exec -it user-service-redis redis-cli KEYS "*"` | claves `users::<id>` | `fase02-09-redis-keys.png` |
| 2.11 | MySQL: `select * from home_energy_tracker_user.user;` + `select * from flyway_schema_history;` | filas + migración `V1__user_table.sql` aplicada | `fase02-10-mysql-user.png` |
| 2.12 | *Delete user* (`DELETE /api/v1/user/{id}`) sobre un usuario creado para la prueba | `204 No Content` | `fase02-11-delete-user.png` |

> 💡 `@CacheEvict` está en update/delete: tras el `PUT` o `DELETE`, repite el `GET` y verás en el log
> que vuelve a consultar la BD. Buena captura para documentar la caché.

---

## 6. Fase 3 — Device Service (:8081)

| # | Request Postman (carpeta *Device Service*) | Resultado esperado | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 3.1 | Swagger UI `http://localhost:8081/swagger-ui.html` | Grupo **devices** con sus operaciones + enum `DeviceType` | `fase03-01-swagger-device.png` |
| 3.2 | *Create device* (`POST /api/v1/device`, `type: LIGHT`, `userId` existente) | `201 Created` + `id` | `fase03-02-create-device.png` |
| 3.3 | **Anota el `deviceId` creado y actualízalo en las variables de Postman** (`deviceId`) | Necesario para las Fases 5, 7 y 8 | `fase03-03-postman-deviceId.png` |
| 3.4 | *List devices (paginated)* (`GET /api/v1/device?page=0&size=10`) | `200` + `PageResponse` | `fase03-04-page-devices.png` |
| 3.5 | *Search devices* (`type=LIGHT&location=Madrid&userId=…`) | `200` filtrando por Specification | `fase03-05-search-devices.png` |
| 3.6 | *Get device by id* · *Update device* | `200` / `200` con cambios | `fase03-06-update-device.png` |
| 3.7 | *List devices by user id* (`GET /api/v1/device/user/{{userId}}?page=0&size=10`) | `200` con los devices del usuario | `fase03-07-devices-by-user.png` |
| 3.8 | Redis (`devices::…`) + MySQL (`select * from home_energy_tracker_device.device;`) | caché + persistencia | `fase03-08-redis-mysql-device.png` |
| 3.9 | *Delete device* sobre un device de prueba | `204 No Content` | `fase03-09-delete-device.png` |

> ⚠️ **Importante para el pipeline de alertas:** el `deviceId` que ingieras en la Fase 5 debe existir
> en device-service **y pertenecer al usuario** con alertas activadas, porque usage-service resuelve
> `deviceId → userId` vía `GET /api/v1/device/{id}` antes de comprobar el umbral.

---

## 7. Fase 4 — Keycloak + API Gateway (:9000)

| # | Request Postman | Resultado esperado | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 4.1 | *Security (Keycloak)* → *Keycloak OpenID configuration* | `200` con `issuer`, `jwks_uri`, `token_endpoint` | `fase04-01-keycloak-discovery.png` |
| 4.2 | *Get access token (client credentials)* | `200` + `access_token` (no se guarda en variable) | `fase04-02-client-credentials.png` |
| 4.3 | *Get access token (password grant)* → script de test guarda `accessToken` | `200` + *test passed* "Access token stored in the accessToken collection variable" | `fase04-03-password-grant.png` |
| 4.4 | *Refresh access token* | `200` con nuevo `access_token` | `fase04-04-refresh-token.png` |
| 4.5 | Colección → *Authorization* → Bearer `{{accessToken}}` (heredada) | Captura de la config de auth de la colección (sin mostrar el token completo) | `fase04-05-postman-bearer.png` |
| 4.6 | *Get user by id (via gateway)* `GET :9000/api/v1/user/{{userId}}` | `200` (ruteado a :8080) | `fase04-06-gateway-get-user.png` |
| 4.7 | *List users (paginated, via gateway)* | `200` | — |
| 4.8 | *Get device by id (via gateway)* | `200` (ruteado a :8081) | `fase04-07-gateway-get-device.png` |
| 4.9 | Misma request **sin** cabecera `Authorization` | `401 Unauthorized` | `fase04-08-gateway-401.png` |
| 4.10 | Swagger agregado del gateway `http://localhost:9000/swagger-ui.html` | *Select a definition*: User / Device / Ingestion / Insight / Usage | `fase04-09-gateway-swagger.png` |
| 4.11 | `GET http://localhost:9000/actuator/health` | `200` + estado de los 5 circuit breakers | `fase04-10-gateway-health.png` |
| 4.12 | `GET http://localhost:9000/actuator/circuitbreakers` | `200` con `userServiceCircuitBreaker`, `deviceServiceCircuitBreaker`, … en `CLOSED` | `fase04-11-circuitbreakers-closed.png` |

> 🔎 Rutas del gateway: `/api/v1/user/**`, `/api/v1/device/**`, `/api/v1/ingestion/**`,
> `/api/v1/insight/**`, `/api/v1/usage/**`. Rutas **públicas**: `/actuator/**`, `/swagger-ui/**`,
> `/docs/**`, `/v3/api-docs/**`. **alert-service no está ruteado** (no expone REST).
> 💡 El token *client credentials* también sirve para las requests del gateway: úsalo si prefieres
> no tocar Keycloak.

---

## 8. Fase 5 — Ingestion → Kafka

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 5.1 | Swagger de ingestion `http://localhost:8082/swagger-ui.html` | Grupo **ingestion** + ejemplo de body | `fase05-01-swagger-ingestion.png` |
| 5.2 | *Ingest energy usage* (directo `:8082`) | `201 Created` (sin body) | `fase05-02-ingest-201.png` |
| 5.3 | **Timestamp actual obligatorio** | El body del collection trae `2026-10-04T15:30:45Z`; **usa `{{$isoTimestamp}}`** (variable dinámica de Postman) o la fecha de hoy, porque la agregación de alertas solo mira **la última hora** y el campo valida `@PastOrPresent` | `fase05-03-ingest-body.png` |
| 5.4 | Repetir la ingestión 5-10 veces (Collection Runner con 10 iteraciones) para tener volumen | Logs de ingestion: `Sending energy usage event...` / `Event published` | `fase05-04-ingestion-logs.png` |
| 5.5 | Kafka UI `http://localhost:8070` → *Topics* → `energy-usage` | Contador de mensajes + particiones | `fase05-05-kafka-topic-energy-usage.png` |
| 5.6 | Kafka UI → *Topics* → `energy-usage` → *Messages* → abrir un mensaje | Payload JSON del `EnergyUsageEvent` (deviceId, energyConsumed, timestamp) | `fase05-06-kafka-message-json.png` |
| 5.7 | Kafka UI → *Consumers* → grupo `usage-service` | Offsets consumidos / lag 0 | `fase05-07-kafka-consumer-offsets.png` |
| 5.8 | Log de usage-service | `Received energy usage event: …` y escritura en InfluxDB | `fase05-08-usage-consumes.png` |
| 5.9 | *(Opcional)* *Ingest energy usage (via gateway)* `:9000/api/v1/ingestion` con Bearer | `201` — demuestra el ruteo del pipeline de entrada | `fase05-09-ingest-via-gateway.png` |

> ⚠️ El `deviceId` del body debe ser el del device creado en la Fase 3 (el collection usa `42`, que
> probablemente no exista → el evento se guardará en Influx pero **nunca generará alerta** porque
> usage-service no podrá resolver su dueño).

---

## 9. Fase 6 — InfluxDB

> ⚠️ **El bucket está vacío ahora mismo.** Esta fase solo tiene sentido **después** de la Fase 5.

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 6.1 | Login `http://localhost:8072` (admin/admin123) | Pantalla inicial de InfluxDB | `fase06-01-influx-login.png` |
| 6.2 | *Data Explorer* → bucket `usage-bucket` | Bucket con la org `home-energy-tracker` | `fase06-02-influx-bucket.png` |
| 6.3 | Filtros: `_measurement = energy_usage`, `_field = energyConsumed`, `_value` + `group by deviceId` → `Aggregate Function: sum` | Serie temporal con los datos ingeridos | `fase06-03-influx-data-explorer.png` |
| 6.4 | Botón *Script Editor* | El Flux generado (útil para el README de usage-service) | `fase06-04-influx-flux-script.png` |
| 6.5 | Pestaña *Raw Data* / vista tabular | Puntos con `deviceId`, `energyConsumed`, `_time` | `fase06-05-influx-raw-data.png` |
| 6.6 | *Load Data → Buckets* | Lista de buckets (`usage-bucket`, `_monitoring`, `_tasks`) | — |

Consulta Flux equivalente (para documentación, vía CLI/curl):

```flux
from(bucket: "usage-bucket")
  |> range(start: -1h)
  |> filter(fn: (r) => r["_measurement"] == "energy_usage")
  |> filter(fn: (r) => r["_field"] == "energyConsumed")
  |> group(columns: ["deviceId"])
  |> sum(column: "_value")
```

---

## 10. Fase 7 — Usage Service (:8083)

| # | Request Postman (carpeta *Usage Service*) | Resultado esperado | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 7.1 | Swagger UI `http://localhost:8083/swagger-ui.html` | Grupo **usage** con `GET /api/v1/usage/{userId}` | `fase07-01-swagger-usage.png` |
| 7.2 | *Get usage by user* `GET :8083/api/v1/usage/{{userId}}?days=3` | `200` + `UsageDto` con cada device y su `energyConsumed` agregado | `fase07-02-usage-response.png` |
| 7.3 | Repetir con `days=1` y `days=30` | Los agregados cambian según la ventana | `fase07-03-usage-days.png` |
| 7.4 | *Get usage for user (via gateway)* `:9000/api/v1/usage/{{userId}}?days=3` con Bearer | `200` (ruteado a :8083) | `fase07-04-usage-via-gateway.png` |
| 7.5 | Usuario sin devices (o id inexistente) | `200` con `devices: []` / comportamiento documentado | — |
| 7.6 | `days=0` | `400 Bad Request` (validación `@Min(1)`) | `fase07-05-usage-400.png` |

> 🔎 La respuesta combina **device-service** (`GET /api/v1/device/user/{id}`) + **InfluxDB**
> (suma por device). Si device-service está caído verás la lista vacía (el cliente traga el error
> con un `warn`), buen caso para capturar el log.

---

## 11. Fase 8 — Alerting E2E (Kafka → Mailpit → MySQL)

> **La fase estrella.** Objetivo: demostrar que una lectura real dispara un email y queda auditada.
> Recuerda la lógica del `@Scheduled(cron = "*/10 * * * * *")` de usage-service:
> suma el consumo **de la última hora** de todos los devices del usuario y lo compara con
> `energyAlertingThreshold` (`totalConsumption > threshold` ⇒ alerta). Solo evalúa usuarios con
> `alerting = true`.

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 8.1 | Crear usuario de alertas | `POST /api/v1/user` con `{"name":"Alert","surname":"Demo","email":"demo.alert@example.com","address":"Calle Demo 1","alerting":true,"energyAlertingThreshold":1.0}` → **anota su `id`** | `fase08-01-create-alert-user.png` |
| 8.2 | Crear device del usuario | `POST /api/v1/device` con `{"name":"Alert Plug","type":"LIGHT","location":"Cocina","userId":<id>}` → **anota su `deviceId`** | `fase08-02-create-alert-device.png` |
| 8.3 | Ingestionar lectura por encima del umbral | `POST /api/v1/ingestion` con `{"deviceId":<deviceId>,"energyConsumed":2.5,"timestamp":"<ahora>"}` | `fase08-03-ingest-alert.png` |
| 8.4 | Esperar ≤10 s y mirar el log de usage-service | `ALERT: User ID <id> exceeded threshold. consumption=2.5, threshold=1.0` | `fase08-04-usage-alert-log.png` |
| 8.5 | Kafka UI → *Topics* → `energy-alerts` → *Messages* | `AlertingEvent` con `userId`, `message: "Energy consumption threshold exceeded"`, `threshold`, `energyConsumed`, `email` | `fase08-05-kafka-energy-alerts.png` |
| 8.6 | Mailpit `http://localhost:8025` → abrir el correo | Asunto `Energy Usage Alert for User <id>`; cuerpo con *Alert / Threshold / Energy Consumed*; remitente `noreply@pgs.com` | `fase08-06-mailpit-alerta.png` |
| 8.7 | Log de alert-service | `Received alert event: …` → `Sending email to: demo.alert@example.com` → `Email sent to: …` | `fase08-07-alert-logs.png` |
| 8.8 | MySQL: `select * from home_energy_tracker_alert.alert order by id desc limit 5;` | Fila con `sent=1`, `user_id`, `message`, `threshold`, `energy_consumed`, `email`, `created_at` | `fase08-08-mysql-alert-audit.png` |
| 8.9 | **Contraprueba**: `PATCH /api/v1/user/<id>/alerting?enabled=false` + nueva lectura + esperar 10 s | **No** aparece email nuevo ni fila nueva (el flag desactiva la alerta) | `fase08-09-alerting-disabled.png` |
| 8.10 | Reactivar: `?enabled=true` y repetir 8.3 | Vuelve a llegar el email (y una nueva fila en `alert`) | — |
| 8.11 | *(Opcional)* Umbral por debajo del consumo acumulado: subir el umbral a `100` y repetir | **No** hay alerta (consumo ≤ umbral) → `log.debug("User ID x is within threshold…")` | `fase08-10-within-threshold-log.png` |

**Cómo forzar el DLT (opcional, ver [limitaciones](#20-limitaciones-conocidas-y-notas)):** con el
código actual **no es alcanzable por el flujo normal** (EmailService captura `MailException` y el
listener ignora eventos incompletos sin lanzar excepción). Solo se puede demostrar modificando
temporalmente el código (ver sección 19) o inyectando un mensaje inválido a mano:

```bash
# Publicar un JSON que no case con AlertingEvent (poison message)
docker exec -i kafka bash -c "echo '{\"notAValidField\":true}' | /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic energy-alerts"
```

Si quieres esa captura, documenta primero el comportamiento observado (log del consumidor / si
aparece `energy-alerts-dlt` en Kafka UI) y anótalo como hallazgo.

---

## 12. Fase 9 — Insight Service + Ollama (:8085)

> Requisitos: **Ollama corriendo** con el modelo `deepseek-coder` (ya descargado) y **datos en Influx**
> para el usuario consultado (el servicio pide a usage-service los últimos 3 días).

| # | Request Postman (carpeta *Insight Service*) | Resultado esperado | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 9.1 | Swagger UI `http://localhost:8085/swagger-ui.html` | Grupo **insight** con los 2 endpoints | `fase09-01-swagger-insight.png` |
| 9.2 | *Get saving tips* `GET /api/v1/insight/saving-tips/{{userId}}` | `200` + `InsightDto {userId, tips, energyUsage}` con texto generado por el LLM (puede tardar 10-60 s) | `fase09-02-saving-tips.png` |
| 9.3 | *Get overview* `GET /api/v1/insight/overview/{{userId}}` | `200` + análisis del consumo | `fase09-03-overview.png` |
| 9.4 | *Get saving tips (via gateway)* `:9000/api/v1/insight/saving-tips/{{userId}}` con Bearer | `200` | `fase09-04-insight-via-gateway.png` |
| 9.5 | Consola de Ollama / `ollama ps` durante la petición | Modelo `deepseek-coder` cargado y atendiendo | `fase09-05-ollama-running.png` |
| 9.6 | **Error path**: parar Ollama y repetir 9.2 | `500 Internal Server Error` con `ErrorResponse` | `fase09-06-insight-ollama-down.png` |
| 9.7 | **Error path**: `userId` inexistente / usage-service caído | `4xx/5xx` con `ErrorResponse` | `fase09-07-insight-error.png` |

---

## 13. Fase 10 — Observabilidad (Prometheus / Grafana / Actuator)

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 10.1 | Prometheus → *Status → Targets* | 7 jobs `UP`: `api-gateway`, `user-service`, `device-service`, `ingestion-service`, `usage-service`, `alert-service`, `insight-service` | `fase10-01-prometheus-targets.png` |
| 10.2 | Prometheus → *Graph* → `http_server_requests_seconds_count` (por `application`) | Tráfico de las llamadas hechas en las fases anteriores | `fase10-02-prometheus-graph.png` |
| 10.3 | Prometheus → `jvm_memory_used_bytes` o `process_cpu_usage` | Métricas JVM | `fase10-03-prometheus-jvm.png` |
| 10.4 | Grafana `http://localhost:3000` → carpeta *Home Energy Tracker* | Dashboard provisionado visible | `fase10-04-grafana-dashboards.png` |
| 10.5 | Abrir *Home Energy Tracker - Overview* (rango *Last 15 minutes*) | Paneles con datos reales (requests, latencia, JVM…) | `fase10-05-grafana-dashboard.png` |
| 10.6 | Grafana → *Explore* → datasource `Prometheus` | Query manual (p. ej. `sum(rate(http_server_requests_seconds_count[1m])) by (application)`) | `fase10-06-grafana-explore.png` |
| 10.7 | Actuator de un servicio: `/actuator/metrics`, `/actuator/prometheus`, `/actuator/info` | Endpoints expuestos | `fase10-07-actuator-endpoints.png` |
| 10.8 | `http://localhost:9000/actuator/metrics` (gateway) | Métricas del gateway (incl. circuit breaker) | — |

> 💡 Si algún target sale `DOWN` es porque ese micro no está arrancado: es el momento perfecto para
> capturar un `DOWN` y explicar el scraping (opcional).

---

## 14. Fase 11 — Resiliencia: circuit breaker del gateway

Config actual: ventana `COUNT_BASED` de 8 llamadas, `failureRateThreshold=20%`, mínimo 4 llamadas,
`waitDurationInOpenState=5s`, 2 llamadas permitidas en `HALF_OPEN`.

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 11.1 | Con user-service arriba: *Circuit breaker fallback (503)* `GET :9000/api/v1/user/{{userId}}` | `200 OK` (circuito `CLOSED`) | `fase11-01-gateway-200.png` |
| 11.2 | **Parar user-service** (`Ctrl+C` en su terminal) | — | — |
| 11.3 | Repetir la request 6-8 veces seguidas | Primero errores del downstream, después **`503 Service Unavailable`** con body `"User service is down"` | `fase11-02-gateway-503-fallback.png` |
| 11.4 | `GET :9000/actuator/circuitbreakers` | `userServiceCircuitBreaker` en `OPEN` | `fase11-03-circuitbreaker-open.png` |
| 11.5 | `GET :9000/actuator/health` | Health degradado con el detalle del breaker | `fase11-04-gateway-health-degraded.png` |
| 11.6 | Log del gateway | `CircuitBreaker 'userServiceCircuitBreaker' is OPEN` / call not permitted | `fase11-05-gateway-cb-log.png` |
| 11.7 | Rearrancar user-service y esperar >5 s → repetir la request | Vuelve el `200`; breaker pasa a `CLOSED` (tras `HALF_OPEN`) | `fase11-06-circuitbreaker-closed-again.png` |
| 11.8 | Repetir el experimento con device-service o usage-service | Mismo comportamiento por servicio | — |

> ⚠️ El body del fallback es **texto plano** (`"User service is down"`), no el contrato
> `ErrorResponse`: es un detalle interesante para la tabla de *Error Handling* del README del gateway.

---

## 15. Fase 12 — Errores y validaciones

> Todas estas capturas alimentan las tablas **Error Handling** de los READMEs. Un pantallazo por caso.

| # | Caso | Petición | Esperado | 📸 Captura |
| :-- | :--- | :--- | :--- | :--- |
| 12.1 | Body inválido (user) | `POST /api/v1/user` sin `name` / email mal formado | `400` + `ErrorResponse {status,error,message}` con el mensaje de validación | `fase12-01-user-400.png` |
| 12.2 | Email duplicado | `POST /api/v1/user` con un email ya existente | `409 Conflict` | `fase12-02-user-409.png` |
| 12.3 | Usuario inexistente | `GET /api/v1/user/999999` | `404 Not Found` + `ErrorResponse` | `fase12-03-user-404.png` |
| 12.4 | Device inexistente | `GET /api/v1/device/999999` | `404 Not Found` | `fase12-04-device-404.png` |
| 12.5 | Device inválido | `POST /api/v1/device` con `type: "FRIDGE"` (no existe en el enum) | `400` (error de conversión) | `fase12-05-device-400-enum.png` |
| 12.6 | Ingestion sin `deviceId` / `timestamp` futuro | `POST /api/v1/ingestion` con `{"energyConsumed":1}` y con `timestamp` futuro | `400` + mensajes `deviceId must not be null` / `timestamp must not be in the future` | `fase12-06-ingestion-400.png` |
| 12.7 | Ingestion con energía negativa | `{"deviceId":1,"energyConsumed":-1,"timestamp":"<ahora>"}` | `400` (`energyConsumed must be zero or positive`) | `fase12-07-ingestion-400-negative.png` |
| 12.8 | Usage con `days=0` | `GET /api/v1/usage/1?days=0` | `400` | `fase12-08-usage-400.png` |
| 12.9 | Gateway sin token / token caducado | `GET :9000/api/v1/user/1` sin `Authorization` | `401` | `fase12-09-gateway-401.png` |
| 12.10 | Gateway con servicio caído | Ver Fase 11 | `503` | `fase12-10-gateway-503.png` |

---

## 16. Fase 13 — Tests automáticos por servicio

No hay POM raíz: **hay que entrar en cada servicio**. Inventario real de clases de test:

| Servicio | Clases de test | Comando |
| :--- | :---: | :--- |
| user-service | 7 | `cd user-service; .\mvnw.cmd test` |
| device-service | 7 | `cd device-service; .\mvnw.cmd test` |
| ingestion-service | 4 | `cd ingestion-service; .\mvnw.cmd test` |
| usage-service | 3 | `cd usage-service; .\mvnw.cmd test` |
| alert-service | 4 | `cd alert-service; .\mvnw.cmd test` |
| insight-service | 9 | `cd insight-service; .\mvnw.cmd test` |
| api-gateway | 1 | `cd api-gateway; .\mvnw.cmd test` |

| # | Paso | Detalle | 📸 Captura |
| :-- | :--- | :--- | :--- |
| 13.1 | Ejecutar los 7 (`docker` debe estar arriba para los Testcontainers de user/device/alert) | `BUILD SUCCESS` + `Tests run: N, Failures: 0, Errors: 0` | `fase13-01-tests-user.png` … `fase13-07-tests-gateway.png` (una por servicio, o una tabla-resumen) |
| 13.2 | Salida de un test E2E con Testcontainers (`UserServiceIntegrationTest`, `DeviceServiceIntegrationTest`, `AlertKafkaIntegrationTest`) | Contenedores MySQL/Redis/Kafka levantados por el test | `fase13-08-testcontainers-logs.png` |
| 13.3 | Informe HTML opcional (`target/site/surefire-report.html` con `mvn surefire-report:report`) | Resumen por clase | `fase13-09-surefire-report.png` |

> 📌 Los READMEs actuales muestran `mvn test` desde la raíz: **corregirlo** en la tarea de
> documentación (usar `.\mvnw.cmd test` por servicio, o el bucle `for d in …`).

---

## 17. Fase 14 — Limpieza y repetición de la demo

Útil si quieres capturas "desde cero" o repetir la demo de alertas sin ruido.

```bash
# 1) Datos MySQL (¡ojo: borra lo que haya!)
docker exec mysql mysql -uroot -padmin -e "
  TRUNCATE TABLE home_energy_tracker_user.user;
  TRUNCATE TABLE home_energy_tracker_device.device;
  TRUNCATE TABLE home_energy_tracker_alert.alert;"

# 2) Topics Kafka (borrar mensajes de pruebas)
docker exec kafka bash -c "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic energy-usage"
docker exec kafka bash -c "/opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic energy-alerts"
# (se recrean solos al volver a producir; opcional: crear también energy-alerts-dlt)

# 3) Caché Redis
docker exec -it user-service-redis redis-cli FLUSHALL

# 4) Mailpit: botón "Delete all" en http://localhost:8025

# 5) InfluxDB: borrar la medida de pruebas (o eliminar docker/influxdb_data y reiniciar el contenedor)
curl -X POST "http://localhost:8072/api/v2/delete?org=home-energy-tracker&bucket=usage-bucket" \
  -H "Authorization: Token my-token" -H "Content-Type: application/json" \
  -d '{"start":"1970-01-01T00:00:00Z","stop":"2100-01-01T00:00:00Z","predicate":"_measurement=\"energy_usage\""}'
```

| # | Paso | 📸 Captura |
| :-- | :--- | :--- |
| 14.1 | Estado "limpio" antes de repetir (Kafka UI + Mailpit + Influx vacíos) | `fase14-01-estado-limpio.png` |

---

## 18. Checklist maestro de capturas

> Destino: en la tarea de documentación se insertarán como `![alt](img/screenshots/<archivo>)`.
> `ROOT` = README de la raíz; el resto = README de ese servicio.

### 18.1 Infraestructura y arranque

| Captura | Qué muestra | README destino |
| :--- | :--- | :--- |
| `fase00-01-docker-ps.png` | Los 9 contenedores arriba | ROOT (Infrastructure) |
| `fase00-02-mysql-schemas.png` | Las 3 BDs creadas por Flyway | ROOT + user/device/alert (Data Model) |
| `fase00-03-ollama-tags.png` | `deepseek-coder` disponible | insight |
| `fase00-04-keycloak-client-dag.png` | Client con Direct Access Grants | ROOT + api-gateway (Security) |
| `fase00-05-keycloak-user.png` | Usuario de pruebas del realm | api-gateway (Security) |
| `fase00-06-postman-variables.png` | Variables de la colección | ROOT (Postman) |
| `fase01-01..07-*-logs.png` | Logs de arranque de cada micro | cada servicio (Getting Started) |
| `fase01-08-actuator-health.png` | Health de los 7 servicios | ROOT (Observability) |
| `fase01-09-kafka-consumers.png` | Grupos `usage-service`/`alert-service` | ROOT (pipelines) + usage/alert |

### 18.2 CRUD + caché + persistencia

| Captura | Qué muestra | README destino |
| :--- | :--- | :--- |
| `fase02-01-swagger-user.png` | Swagger del user-service | user (API Endpoints) |
| `fase02-02-create-user.png` | `POST` con `201` | user (Request/Response Examples) |
| `fase02-03-list-users.png` / `fase02-04-page-users.png` | Listado y paginación | user |
| `fase02-05-search-users.png` | Filtros dinámicos | user |
| `fase02-06-update-user.png` / `fase02-07-toggle-alerting.png` | `PUT` y `PATCH` alerting | user |
| `fase02-08-redis-cache.png` / `fase02-09-redis-keys.png` | Caché Redis (`users::`) | user (Caching) |
| `fase02-10-mysql-user.png` | Tabla + `flyway_schema_history` | user (Data Model) |
| `fase02-11-delete-user.png` | `204` | user |
| `fase03-01-swagger-device.png` | Swagger del device-service | device |
| `fase03-02-create-device.png` / `fase03-03-postman-deviceId.png` | Crear device y variable `deviceId` | device |
| `fase03-04-page-devices.png` / `fase03-05-search-devices.png` | Paginación y filtros (`type`, `location`) | device |
| `fase03-06-update-device.png` / `fase03-07-devices-by-user.png` | `PUT` y devices por usuario | device |
| `fase03-08-redis-mysql-device.png` | Caché + MySQL | device |
| `fase03-09-delete-device.png` | `204` | device |

### 18.3 Seguridad, gateway, pipelines

| Captura | Qué muestra | README destino |
| :--- | :--- | :--- |
| `fase04-01-keycloak-discovery.png` | `.well-known/openid-configuration` | api-gateway (Security) |
| `fase04-02-client-credentials.png` / `fase04-03-password-grant.png` / `fase04-04-refresh-token.png` | Los 3 flujos de token | api-gateway (Security) |
| `fase04-05-postman-bearer.png` | Auth Bearer heredada | ROOT (Postman) |
| `fase04-06-gateway-get-user.png` / `fase04-07-gateway-get-device.png` | Ruteo por el gateway | api-gateway (Routing) |
| `fase04-08-gateway-401.png` | `401` sin token | api-gateway (Error Handling) |
| `fase04-09-gateway-swagger.png` | Swagger agregado (5 specs) | api-gateway (OpenAPI) |
| `fase04-10-gateway-health.png` / `fase04-11-circuitbreakers-closed.png` | Health + breakers `CLOSED` | api-gateway |
| `fase05-01-swagger-ingestion.png` | Swagger de ingestion | ingestion |
| `fase05-02-ingest-201.png` / `fase05-03-ingest-body.png` | `201` + body con timestamp actual | ingestion |
| `fase05-04-ingestion-logs.png` | Logs del producer fire-and-forget | ingestion |
| `fase05-05-kafka-topic-energy-usage.png` / `fase05-06-kafka-message-json.png` / `fase05-07-kafka-consumer-offsets.png` | Topic, payload y offsets | ROOT (pipeline 1) + usage |
| `fase05-08-usage-consumes.png` / `fase05-09-ingest-via-gateway.png` | Consumo del evento y ruta por gateway | usage / ROOT |
| `fase06-01..05-influx-*.png` | Bucket, Data Explorer, Flux y raw data | ROOT (pipeline 1) + usage (Storage) |
| `fase07-01-swagger-usage.png` / `fase07-02-usage-response.png` / `fase07-03-usage-days.png` | Endpoint y agregados por días | usage (API + Examples) |
| `fase07-04-usage-via-gateway.png` / `fase07-05-usage-400.png` | Gateway + validación | usage |

### 18.4 Alerting, IA, observabilidad y errores

| Captura | Qué muestra | README destino |
| :--- | :--- | :--- |
| `fase08-01-create-alert-user.png` / `fase08-02-create-alert-device.png` | Preparación del escenario de alerta | alert (Getting Started) |
| `fase08-03-ingest-alert.png` | Lectura que supera el umbral | ROOT (pipeline 2) |
| `fase08-04-usage-alert-log.png` | `ALERT: User ID … exceeded threshold` | usage (Alerting) |
| `fase08-05-kafka-energy-alerts.png` | `AlertingEvent` en el topic | ROOT + alert |
| `fase08-06-mailpit-alerta.png` | **Email recibido en Mailpit** | alert (Email) + ROOT |
| `fase08-07-alert-logs.png` | Consumo + envío en alert-service | alert |
| `fase08-08-mysql-alert-audit.png` | Auditoría `sent=1` con contenido | alert (Data Model) |
| `fase08-09-alerting-disabled.png` / `fase08-10-within-threshold-log.png` | Contrapruebas (flag off / umbral no superado) | user + usage |
| `fase09-01-swagger-insight.png` | Swagger de insight | insight |
| `fase09-02-saving-tips.png` / `fase09-03-overview.png` | Respuestas del LLM | insight (Examples) + ROOT (pipeline 3) |
| `fase09-04-insight-via-gateway.png` / `fase09-05-ollama-running.png` | Ruta por gateway y Ollama activo | insight / ROOT |
| `fase09-06-insight-ollama-down.png` / `fase09-07-insight-error.png` | Errores del LLM / upstream | insight (Error Handling) |
| `fase10-01-prometheus-targets.png` | 7 targets `UP` | ROOT (Observability) |
| `fase10-02-prometheus-graph.png` / `fase10-03-prometheus-jvm.png` | Queries de tráfico y JVM | ROOT |
| `fase10-04-grafana-dashboards.png` / `fase10-05-grafana-dashboard.png` | Dashboard provisionado con datos | ROOT (Observability) |
| `fase10-06-grafana-explore.png` / `fase10-07-actuator-endpoints.png` | Explore + actuator | ROOT |
| `fase11-01-gateway-200.png` … `fase11-06-circuitbreaker-closed-again.png` | Ciclo completo del circuit breaker | api-gateway (Circuit Breaker) |
| `fase12-01..10-*.png` | `400`, `404`, `409`, `503` con `ErrorResponse` | cada servicio (Error Handling) |
| `fase13-01..09-*.png` | Resultados de `mvnw test` por servicio | cada servicio (Testing) + ROOT (Testing Strategy) |
| `fase14-01-estado-limpio.png` | Estado limpio para repetir | — (uso interno) |

**Total estimado:** ~60 capturas. Prioriza las marcadas como ROOT/estrella (Fases 4, 5, 6, 8, 10, 11);
las demás son refuerzo.

---

## 19. Cambios de código opcionales (revertir después)

> Ninguno de estos cambios es necesario para el plan. Si los haces, **anótalos y revierte el código**
> al terminar (no forman parte de la tarea de documentación).

| # | Cambio | Para qué | Revertir |
| :-- | :--- | :--- | :--- |
| 19.1 | Descomentar `//@Scheduled(fixedRateString = "${simulation.interval-ms}")` en `ingestion-service/.../simulation/ContinuousDataSimulator.java` | Generar carga automática (10 000 req/min) y tener gráficas bonitas en Grafana | Volver a comentar |
| 19.2 | Hacer que `EmailService.sendEmail` **relance** `MailException` (o quitar el `catch`) | Poder demostrar el retry (3 intentos / 5 s) y el **DLT** `energy-alerts-dlt` | Restaurar el `catch` original |
| 19.3 | Subir el nivel de log a `DEBUG` para `com.pgs.*` (`logging.level.com.pgs=DEBUG`) | Capturar los `log.debug` de agregación, caché y clientes | Quitar la propiedad |
| 19.4 | Añadir una `@Scheduled`/endpoint temporal en alert-service | Forzar un fallo controlado para el DLT | Eliminar |

---

## 20. Limitaciones conocidas y notas

1. **La agregación de alertas mira solo la última hora** (`ONE_HOUR_IN_SECONDS`) y se ejecuta cada
   10 s. Si ingieres lecturas con `timestamp` antiguo (como el `2026-10-04` del collection), InfluxDB
   las guarda pero **no** generan alerta.
2. **Umbral por usuario, no por device:** se suma el consumo de **todos** los devices del usuario en
   la última hora y se compara con `energyAlertingThreshold`.
3. **El DLT no es alcanzable por el flujo normal** (ver 19.2). Si no consigues la captura, documéntalo
   como *known gap* en el README de alert-service.
4. **`alert-service` no expone REST** y **no está ruteado** en el gateway: no busques Swagger ahí.
5. **Password grant deshabilitado** en el realm exportado → usa *client credentials* o habilita
   Direct Access Grants en Keycloak (Fase 0.4).
6. **InfluxDB arranca vacío**: las capturas de Influx/Grafana requieren datos ingeridos en la misma
   sesión (ventana de retención: 1 semana).
7. **Testcontainers** en user/device/alert exige Docker en marcha; los tests E2E son más lentos
   (levanta MySQL/Redis/Kafka propios).
8. **No hay POM raíz**: cualquier comando Maven debe ejecutarse dentro del directorio del servicio.
9. **Retención de Mailpit**: los correos viven en memoria del contenedor; un `docker compose down`
   los borra. Captura antes de reiniciar el stack.
10. **La captura de MySQL que compartiste** (con `sakila`, `test` y la BD `home_energy_tracker`)
    corresponde a otra instancia: apunta tu cliente al contenedor (`127.0.0.1:3306`, root/admin).

---

## 21. Siguiente tarea: integrar las capturas en los READMEs

Cuando este plan esté completado (checklist al 100 %), la tarea de documentación debería:

1. Copiar las capturas a `img/screenshots/` con la nomenclatura de la sección 0.
2. Insertarlas en el **README raíz** (arquitectura, pipelines 1/2/3, infraestructura, Postman, testing)
   y en el **README de cada servicio** (endpoints, ejemplos, data model, caching, error handling,
   testing, roadmap).
3. Corregir en la raíz el snippet `mvn test` → `.\mvnw.cmd test` por servicio (no hay POM raíz).
4. Actualizar `AGENTS.md` §3 (Repository Structure) y la sección *Repository Structure* del README
   raíz si se decide versionar `plan.md` e `img/screenshots/`.
5. Marcar los ítems del **Roadmap** completados y añadir los hallazgos nuevos (p. ej. el DLT no
   alcanzable, el password grant deshabilitado en el realm exportado).

---

---

## 22. Bugs detectados durante las pruebas

> Encontrados al recorrer este plan el **29/10/2026**. Los tres primeros se **arreglaron**
> (Fix 1 + Fix 2 + Fix 3A); el cuarto queda documentado como deuda técnica.
> **Verificación:** `cd usage-service; .\mvnw.cmd test` → **27 tests, 0 fallos**
> (`UsageServiceTest` nuevo con 12 tests y `DeviceClientTest` reescrito contra el contrato real).

### 22.1 🔴 `GET /api/v1/usage/{userId}` devolvía 500 (contrato roto) — **ARREGLADO**

**Síntoma** (directo en `:8083` o a través del gateway, y también en insight):

```
RestClientException: Error while extracting response for type [java.util.List<com.pgs.usage.service.dto.DeviceDto>]
Caused by: MismatchedInputException: Cannot deserialize value of type `java.util.ArrayList<...DeviceDto>`
           from Object value (token `JsonToken.START_OBJECT`)
    at com.pgs.usage.service.client.DeviceClient.getAllDevicesForUser(DeviceClient.java:84)
```

**Causa:** `device-service` responde `GET /api/v1/device/user/{userId}?page&size` con un
**`PageResponse`** (objeto `{content, page, size, totalElements, totalPages, last, first, empty}`),
pero `DeviceClient` llamaba a esa URL **sin** `page`/`size` y deserializaba un **array**.
Los tests no lo veían porque mockeaban un array JSON y una URL sin query params.

**Impacto:** rompía el 100 % de `GET /api/v1/usage/{userId}` y, en cascada, los dos endpoints de
`insight-service` (pipeline 3 de IA).

**Arreglo:**
- nuevo `usage-service/.../dto/PageResponse.java` (espejo del contrato de device-service);
- `DeviceClient.getAllDevicesForUser` recorre **todas** las páginas
  (`?page={page}&size={size}`, `device.service.page-size=100`) acumulando `content` hasta
  `last == true`, con tope de seguridad y `warn` si se alcanza;
- `DeviceClientTest` reescrito con el **JSON real** del `PageResponse`: página única, multi-página,
  `content` vacío, body vacío y `404`.

**Capturas:** `fase12-11-usage-500-before.png` (antes) y `fase07-02-usage-response.png` (después, `200`).

### 22.2 🟡 Ruido de logs con usuarios borrados — **ARREGLADO**

**Síntoma:** tras borrar el usuario `1`, cada 10 s aparecía en el log de `usage-service`:

```
WARN  UsageService : Failed to fetch user for ID: 1
org.springframework.web.client.HttpClientErrorException$NotFound: 404 Not Found:
    {"status":404,"error":"Not Found","message":"User not found with id: 1"}
```

**Causa:** borrar un usuario **no** borra sus devices ni sus lecturas en InfluxDB, así que la
agregación seguía resolviendo `deviceId → userId=1` y pidiendo el usuario. Estaba **manejado**
(el usuario se ignora), pero se registraba como `warn` con traza completa.

**Arreglo:** `UsageService.findUser` captura específicamente `HttpClientErrorException.NotFound`
y lo registra en `debug` sin traza; el resto de fallos siguen en `warn` con la excepción
(no se ocultan errores reales).

**Captura:** `fase08-11-usage-404-debug.png` (log limpio tras el fix).

### 22.3 🟡 Tormenta de alertas: un email cada 10 segundos — **ARREGLADO**

**Síntoma:** en el log de `usage-service`, `ALERT: User ID 2 exceeded threshold...` se repetía en
**cada** ejecución del cron (15:50:32, 15:50:41, 15:50:51, 15:51:01…). Como cada `AlertingEvent`
termina en un email, `alert-service` enviaba un correo cada 10 s y añadía una fila en
`home_energy_tracker_alert.alert` (con cientos ya acumuladas de pruebas anteriores).

**Causa:** `checkThresholdAndAlert` no tenía cooldown ni deduplicación: mientras el consumo de la
última hora siguiera por encima del umbral, cada ejecución publicaba un evento nuevo.

**Arreglo (Fix 3A):** cooldown configurable por usuario.

- `app.alerting.cooldown-minutes=60` (propiedad nueva) + `Map<Long, Instant> lastAlertSentAt`
  (un timestamp por usuario, purgado al expirar);
- se inyecta un `Clock` (`config/ClockConfig.java`) para poder controlar el tiempo en los tests
  (`MutableClock`);
- **limitación documentada:** el estado vive **en memoria** → se pierde al reiniciar el servicio y
  no se comparte entre instancias (tras un reinicio puede reenviarse un aviso por usuario).

**Capturas:** `fase08-12-mailpit-antes-cooldown.png` (bandeja inundada) y
`fase08-13-mailpit-con-cooldown.png` (un único correo por usuario).

### 22.4 ⚪ Datos huérfanos al borrar un usuario — **NO ARREGLADO (deuda técnica)**

Borrar un usuario **no** elimina sus devices (device-service) ni sus lecturas (InfluxDB), así que
siguen contándose en la agregación. Se ha dejado **fuera de alcance** a propósito (Fix 4 descartado)
y queda anotado en el roadmap del README de `usage-service`. Mientras no se implemente, el
comportamiento es: el usuario se ignora silenciosamente (log `debug`) y sus devices/mediciones
siguen ocupando espacio en InfluxDB.

**Captura:** `fase12-12-orphan-devices.png` (devices del usuario borrado siguen existiendo).

### 22.5 Checklist de verificación del arreglo

| # | Comprobación | Resultado |
| :-- | :--- | :--- |
| 1 | `usage-service` → `.\mvnw.cmd test` | ✅ 27 tests, 0 fallos (12 nuevos en `UsageServiceTest`) |
| 2 | `DeviceClientTest` valida el **contrato paginado real** (URL con `?page&size`, `PageResponse`, multi-página) | ✅ 5 tests |
| 3 | Cooldown: se suprime el aviso repetido, vuelve al expirar y es **por usuario** | ✅ 3 tests |
| 4 | Usuario borrado (`404`) y fallo del User Service (`500`) no rompen la agregación | ✅ 2 tests |
| 5 | Manual, **en vivo** — instancia nueva del código arreglado en `:8093` → `GET /api/v1/usage/2?days=3` | ✅ `200` con `devices[]` y `energyConsumed` (307.03, 272.81, 335.42…) |
| 5b | Manual, **en vivo** — instancia antigua (código previo, misma BD) en `:8083`, misma petición | ❌ `500` `{"status":500,"error":"Internal Server Error","message":"Unexpected error"}` → antes/después en el mismo entorno |
| 6 | Manual, **en vivo** — cooldown con el cron cada 10 s (5 ejecuciones en 38 s) | ✅ 1 alerta por usuario (5 usuarios) en la **primera** ejecución y **0** en las 4 siguientes |
| 7 | Manual: `GET :9000/api/v1/insight/saving-tips/{id}` → `200` | ⏳ pendiente: requiere Ollama arrancado y **reiniciar** `usage-service` en `:8083` con el código nuevo |

---

## 23. Bug: la petición de insight se cuelga cuando Ollama está caído

> Detectado el **29/10/2026** al llamar por el gateway a `GET /api/v1/insight/saving-tips/{userId}`.
> **Arreglado** (Fase 0 desbloqueo de Ollama + Fase 1 fail-fast y `503` honesto).

### 23.1 Síntoma

El log de `insight-service` se llenaba de WARN de reintentos y **la petición HTTP no devolvía nada**:

```
INFO  InsightService : Calling Ollama for userId 2 with total usage 6026.83
WARN  o.s.a.r.a.SpringAiRetryAutoConfiguration : Retry error. Retry count:3
org.springframework.web.client.ResourceAccessException: I/O error on POST request for
  "http://localhost:11434/api/chat": Connection refused: getsockopt: localhost/127.0.0.1:11434
WARN  ... Retry error. Retry count:4      (10 s después)
```

**Nota positiva:** `total usage 6026.83` demuestra que el **Fix 1** (§22.1) funcionó end-to-end: el Usage Service devolvió los datos correctamente.

### 23.2 Causas

1. **Ollama caído (entorno).** `netstat | grep 11434` → sin coincidencias y `curl :11434/api/tags` → `Connection refused`, pero el proceso `ollama app.exe` seguía vivo (zombi): `ollama list` respondía `existing instance found… Error: timed out waiting for server to start` y `app.log` registraba `ollama server not ready after retries`.
2. **Amplificador (configuración, bug real).** Spring AI 2.0.1 reintenta con los valores por defecto verificados en `spring-ai-autoconfigure-retry-2.0.1.jar`: `spring.ai.retry.max-attempts=10`, `backoff.initial-interval=2000ms`, `multiplier=5` y `max-interval` **sin valor por defecto (sin tope)** → esperas de 2s, 10s, 50s, 250s, 1250s… La petición no termina nunca (no hay timeout en Tomcat ni en la ruta del gateway), así que el cliente se queda colgado en vez de recibir un error.
3. **Mensaje engañoso (gateway).** `insightServiceCircuitBreaker` acumuló `bufferedCalls=2, failedCalls=2`; al abrirse (umbral 20 %, mínimo 4 llamadas) el gateway responde su fallback **`503 "Insight service is down"`**, culpando al servicio equivocado (quien falla es Ollama).

### 23.3 Arreglo

**Fase 0 (entorno):**

```powershell
taskkill /F /IM "ollama app.exe"      # mata el proceso zombi
ollama serve                          # arranca el servidor en :11434
curl http://localhost:11434/api/tags  # verificación
```

El modelo `deepseek-coder:latest` ya estaba descargado (776 MB) y un chat de prueba respondió correctamente.

**Fase 1 (código):**

- `insight-service/application.properties`: `spring.ai.retry.max-attempts=2`, `backoff.initial-interval=1s`, `multiplier=2`, `max-interval=2s` → **de "nunca termina" a ~1-2 s**.
- Nueva excepción de dominio `DependencyUnavailableException` (`dependency` + `detail`).
- `InsightService`: nuevos helpers `fetchUsage(...)` y `callChatModel(...)` que traducen `RestClientException` (conexión rechazada, timeout, `5xx`) en esa excepción, nombrando la dependencia (`Usage Service` / `Ollama`).
- `GlobalExceptionHandler`: nuevo handler → **`503 Service Unavailable`** con `{"status":503,"error":"Service Unavailable","message":"Ollama is unavailable: Connection refused"}`; el `500` genérico se mantiene para errores inesperados.
- `InsightController`: `@ApiResponse(responseCode = "503", …)` documentado en los dos endpoints.
- Tests: **38 tests, 0 fallos** (+4 nuevos: 3 de traducción a `DependencyUnavailableException` y 1 del contrato `503` en `GlobalExceptionHandlerTest`).

### 23.4 Verificación

| # | Comprobación | Resultado |
| :-- | :--- | :--- |
| 1 | `insight-service` → `.\mvnw.cmd test` | ✅ 38 tests, 0 fallos |
| 2 | Ollama arriba → `GET :8095/api/v1/insight/saving-tips/2` (instancia nueva) | ✅ `200` con `tips` + `energyUsage` |
| 3 | Ollama caído → misma llamada | ✅ **`503` en ~1-2 s** con `"Ollama is unavailable: …"` (antes: colgada) |
| 4 | Ollama arriba → `GET :9000/api/v1/insight/saving-tips/2` (gateway) | ❌ `503 "Device service is down"` en **1.01 s** — **no es un fallo de insight**: son dos bugs del gateway (§24) |
| 4b | Mismo gateway pero con `resilience4j.timelimiter.configs.default.timeout-duration=30s` (instancia de prueba en `:9001`) | ✅ **`200` en 1.04 s** con `tips` + `energyUsage` |
| 5 | Reiniciar `insight-service` en `:8085` para que el gateway use el código nuevo | ⚠️ manual: la instancia en ejecución tenía el código anterior |

### 23.5 Recomendación (no aplicada)

El modelo configurado es `deepseek-coder` (modelo de **código**): en la prueba respondió *"tell me how can I assist you with your computer science-related questions"*. Para consejos energéticos funcionan mejor los modelos generales (`llama3.2`, `qwen2.5`, `mistral`); cambiarlo es solo una propiedad (`spring.ai.ollama.chat.options.model`) más `ollama pull`.

---

## 24. Bugs del gateway detectados al verificar insight

> Detectados el **29/10/2026** durante la verificación en vivo de §23 y **ARREGLADOS** en la misma
> sesión (tras aprobarlo el usuario): `resilience4j.timelimiter.configs.default.timeout-duration=30s`
> + un fallback propio por servicio.
> **Impacto que tenían:** la ruta `/api/v1/insight/**` del gateway **no podía devolver `200`**, ni
> siquiera con Ollama funcionando, y el cliente recibía un `503` que nombraba al servicio equivocado.

### 24.1 🔴 El circuit breaker del gateway corta las llamadas a los 1 s (`TimeLimiter` por defecto)

**Evidencia:**

| Prueba | Resultado |
| :--- | :--- |
| `GET :8085/api/v1/insight/saving-tips/2` (directo, backend sano) | ✅ `200` en **1.44 s** |
| `GET :9000/api/v1/insight/saving-tips/2` (gateway, mismo token de Keycloak) | ❌ `503` en **~1.01 s**, siempre el mismo tiempo |
| `GET :9000/api/v1/usage/2?days=3`, `/user/2`, `/device/1` | ✅ `200` en 20-50 ms (rutas rápidas) |
| `resilience4j.timelimiter` en `api-gateway/src/main/resources/` | **no existe** → se aplica el `TimeLimiter` por defecto de Resilience4j (**1 s**) |
| `GET :9001/api/v1/insight/saving-tips/2` (gateway de prueba con `timeout-duration=30s`) | ✅ **`200` en 1.04 s**, `failedCalls=0` |

**Causa:** `spring-cloud-starter-circuitbreaker-resilience4j` envuelve cada ruta en un
`TimeLimiter` y, sin configuración explícita, usa el **timeout de 1 segundo**. Una llamada al LLM
tarda 1-6 s, así que **todas** las llamadas a insight se cortan, se registran como fallo y el
circuit breaker termina abriéndose (`insightServiceCircuitBreaker` llegó a `HALF_OPEN`/`OPEN`).

**Arreglo propuesto (1 línea de configuración, sin tocar código):**

```properties
resilience4j.timelimiter.configs.default.timeout-duration=30s
```

### 24.2 🟡 Las 5 rutas de fallback comparten path e id (`/fallbackRoute`) → mensaje del servicio equivocado

**Evidencia:** las cinco clases de `api-gateway/route/` registran
`.route(RequestPredicates.path("/fallbackRoute"), …)` con el **mismo id** (`"fallbackRoute"`) y
cuerpos distintos (`"Device/Ingestion/Insight/Usage/User service is down"`). Comprobado con el
gateway en marcha:

```bash
$ curl -H "Authorization: Bearer $TOKEN" http://localhost:9000/fallbackRoute
Device service is down

$ curl -H "Authorization: Bearer $TOKEN" http://localhost:9000/api/v1/insight/saving-tips/2
Device service is down          # ← llamada a INSIGHT, con el CB abierto
HTTP 503 en 1.01s
```

**Causa:** solo la **primera** ruta de fallback registrada resuelve el path, así que cualquier
circuit breaker abierto responde con el mensaje de *device*, apuntando a un servicio que funciona.

**Arreglo propuesto:** un path y un id por servicio (`/fallbackRoute/insight`, `/fallbackRoute/device`,
…) y apuntar cada `circuitBreaker(id, URI.create("forward:/fallbackRoute/<service>"))` a su propio
fallback; actualizar `api-gateway/README.md` y el checklist de capturas de este plan.

### 24.3 Consecuencia conjunta

Mientras **24.1** no se arreglase, la ruta de insight del gateway **no podía devolver `200`** (el LLM
tarda más de 1 s) y el cliente recibía un `503` con el texto **"Device service is down"** (**24.2**):
los dos bugs se reforzaban para producir el mensaje más confuso posible.

### 24.4 Arreglo aplicado y verificación

**Cambios:**

- `api-gateway/src/main/resources/application.properties`: `resilience4j.timelimiter.configs.default.timeout-duration=30s`.
- Los 5 ficheros de `api-gateway/route/`: cada uno apunta su `circuitBreaker(...)` a `forward:/fallbackRoute/<service>` y registra su fallback con id y path propios (`/fallbackRoute/user|device|ingestion|insight|usage`).
- Nuevo test `api-gateway/src/test/java/com/pgs/api/gateway/route/ServiceFallbackRoutesTest.java` (6 tests con `MockMvcBuilders.routerFunctions`): cada fallback responde `503` con **su** mensaje y el path compartido antiguo `/fallbackRoute` ya **no** está registrado (`404`).
- Docs: `api-gateway/README.md` (tabla de rutas, circuit breaker + *time limiter*, testing, roadmap), `AGENTS.md` §9 y este §24.

**Verificación en vivo** (gateway de prueba en `:9001` con el código nuevo, mismo Keycloak y mismos servicios):

| # | Comprobación | Resultado |
| :-- | :--- | :--- |
| 1 | `GET :9001/api/v1/insight/saving-tips/2` con token | ✅ **`200` en 5.70 s** con `tips` + `energyUsage` (antes: `503` en 1.01 s) |
| 2 | `GET :9001/fallbackRoute/insight` | ✅ `503` → `Insight service is down` (antes: `Device service is down`) |
| 3 | `GET :9001/fallbackRoute` (path antiguo) | ✅ `404` (ya no existe) |
| 4 | `GET :9001/api/v1/ingestion/ping` ×6 (ingestion `:8082` caído → CB abierto) | ✅ `503` → `Ingestion service is down` |
| 5 | `api-gateway` → `.\mvnw.cmd test` | ✅ **7 tests, 0 fallos** |
| 6 | Reiniciar el gateway en `:9000` para aplicar el arreglo | ⚠️ manual: la instancia en ejecución tiene el código anterior |

<!--PLAN-FIN-->
