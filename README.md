# kopring-bricks

Spring Boot 기반 Kotlin 애플리케이션에서 반복해서 쓰는 설정을 starter 형태로 제공하는 라이브러리 모음입니다.

## Modules

```text
http-client/
  vt-rest-client-autoconfigure
  vt-rest-client-starter
jdbc-client/
  vt-jdbc-client-autoconfigure
  vt-jdbc-client-starter
observability/
  logging-observation-autoconfigure
  logging-observation-starter
  micrometer-tracing-autoconfigure
  micrometer-tracing-starter
web/
  problem-details-autoconfigure
  problem-details-starter
  concurrency-control-autoconfigure
  concurrency-control-starter
  webmvc-error-autoconfigure
  webmvc-error-starter
cache/
  caffeine-cache-autoconfigure
  caffeine-cache-starter
resilience/
  resilience4j-autoconfigure
  resilience4j-starter
audit/
  audit-log-autoconfigure
  audit-log-starter
event-sourcing/
  event-sourcing-autoconfigure
  event-sourcing-starter
messaging/
  outbox-autoconfigure
  outbox-starter
support/
  jdbc-autoconfigure
test-support/
  kopring-bricks-test-support
samples/
  todo-api
```

`*-autoconfigure` 모듈은 실제 auto-configuration을 제공하고, `*-starter` 모듈은 애플리케이션에서 가져다 쓰는 starter 의존성입니다.
`support/*` 모듈은 starter 구현에서 공유하는 내부 helper이며 애플리케이션이 직접 의존하는 대상이 아닙니다.
`kopring-bricks-test-support`는 starter를 소비하는 애플리케이션 테스트에서 쓰는 recording/fake helper를 제공합니다.

애플리케이션 개발 에이전트가 이 라이브러리를 소비하거나 라이브러리 변경 요청을 넘겨야 할 때는 [Application Agent Guide](docs/application-agent-guide.md)를 참고하세요.

Feature flag, rollout, policy rule 같은 룰 기반 분기 starter는 아직 구현하지 않았습니다. 다음 후보의 경계는 [ADR 0001: Rule Decision Starter Boundary](docs/adr/0001-rule-decision-starter.md)에 정리되어 있습니다.

## HTTP Client

`vt-rest-client-starter`는 Spring `RestClient`가 JDK `HttpClient` 기반 request factory를 사용하도록 자동 구성합니다. 기본적으로 JDK virtual thread executor를 `HttpClient`에 연결하고, Spring Boot의 `RestClient.Builder`에는 `RestClientCustomizer`를 통해 같은 request factory를 적용합니다.

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:vt-rest-client-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    http-client:
      connect-timeout: 3s
      read-timeout: 10s
      follow-redirects: false
      compression-enabled: false
      virtual-threads:
        enabled: true
        thread-name-prefix: kopring-bricks-http-
      clients:
        github:
          base-url: https://api.github.com
          default-headers:
            Accept:
              - application/vnd.github+json
```

### Default RestClient

```kotlin
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class UserApi(
    builder: RestClient.Builder,
) {
    private val client = builder
        .baseUrl("https://api.example.com")
        .build()
}
```

### Named RestClient

```kotlin
import me.sensibile.kopringbricks.httpclient.autoconfigure.VtRestClientFactory
import org.springframework.stereotype.Service

@Service
class GithubApi(
    factory: VtRestClientFactory,
) {
    private val client = factory.restClient("github")
}
```

Named clients reuse the auto-configured `RestClient.Builder`, then apply the configured `base-url` and `default-headers`.

## JDBC Client

`vt-jdbc-client-starter`는 Spring `JdbcClient`를 자동 구성하고, JDBC 작업을 virtual thread executor에서 실행할 수 있는 얇은 helper를 제공합니다. JDBC 드라이버 호출은 blocking I/O이므로 non-blocking으로 바뀌지는 않지만, virtual thread를 통해 blocking JDBC 작업을 platform thread 고갈 없이 다루기 쉬워집니다.

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:vt-jdbc-client-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    jdbc-client:
      enabled: true
      operations-enabled: true
      virtual-threads:
        enabled: true
        thread-name-prefix: kopring-bricks-jdbc-
```

### Default JdbcClient

```kotlin
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val jdbcClient: JdbcClient,
) {
    fun count(): Int =
        jdbcClient.sql("select count(*) from users")
            .query(Int::class.java)
            .single()
}
```

### Virtual Thread Helper

```kotlin
import me.sensibile.kopringbricks.jdbcclient.autoconfigure.VtJdbcClientOperations
import org.springframework.stereotype.Service

@Service
class UserQueryService(
    private val jdbc: VtJdbcClientOperations,
) {
    fun countAsync() =
        jdbc.submit { client ->
            client.sql("select count(*) from users")
                .query(Int::class.java)
                .single()
        }
}
```

## Concurrency Control

`concurrency-control-starter`는 관리 API와 룰 변경 API에서 반복되는 optimistic concurrency control primitives를 제공합니다. `If-Match` 기반 버전 검증, ETag 생성, idempotency key 추출, 표준 `ApiException` 기반 409/412/428 예외를 제공합니다.

기본 동작:

- `ETagGenerator` 자동 구성
- `IfMatchValidator` 자동 구성
- `IdempotencyKeyResolver` 자동 구성
- `VersionConflictException`으로 `409 Conflict` 표현
- `PreconditionFailedException`으로 `412 Precondition Failed` 표현
- `PreconditionRequiredException`으로 `428 Precondition Required` 표현
- `IdempotencyConflictException`으로 idempotency key 충돌 표현

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:concurrency-control-starter:0.0.1-SNAPSHOT")
    implementation("me.sensibile:webmvc-error-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    concurrency-control:
      enabled: true
      etag:
        strong: true
      idempotency:
        header-name: Idempotency-Key
```

### If-Match Validation

```kotlin
import me.sensibile.kopringbricks.web.concurrency.autoconfigure.IfMatchValidator
import me.sensibile.kopringbricks.web.concurrency.autoconfigure.ETagGenerator
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class FeatureRuleController(
    private val rules: FeatureRuleService,
    private val etags: ETagGenerator,
    private val ifMatchValidator: IfMatchValidator,
) {
    @PutMapping("/feature-rules/{id}")
    fun update(
        @PathVariable id: String,
        @RequestHeader(name = HttpHeaders.IF_MATCH, required = false) ifMatch: String?,
    ): ResponseEntity<FeatureRuleResponse> {
        val current = rules.get(id)
        ifMatchValidator.requireMatch(ifMatch, current.version)

        val updated = rules.update(id)

        return ResponseEntity.ok()
            .eTag(etags.generate(updated.version))
            .body(FeatureRuleResponse.from(updated))
    }
}
```

## Event Sourcing

`event-sourcing-starter`는 도메인 이벤트를 append-only stream으로 저장하고 다시 읽어 상태를 재구성하기 위한 얇은 저장소/템플릿 API를 제공합니다. Aggregate 모델, 도메인 이벤트 클래스, projection, 외부 발행 방식은 애플리케이션이 소유하고, starter는 반복되는 저장소 배선과 optimistic stream version 검증만 담당합니다.

`samples:todo-api`는 애플리케이션이 제공한 in-memory `EventStore` 위에 `EventSourcingTemplate`을 구성하고, todo 생성/완료 시점에 `todo.created`, `todo.completed` 이벤트를 저장하는 흐름을 보여줍니다.

기본 동작:

- `EventStore` 계약 제공
- `EventSourcingTemplate` 자동 구성
- PostgreSQL datasource로 감지되면 `JdbcEventStore` 구성
- 앱에서 `EventStore` 또는 `EventSourcingTemplate` Bean을 등록하면 기본 구현 back off
- JDBC 저장소 조건이 맞지 않으면 내구성 없는 fallback 저장소를 만들지 않음

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:event-sourcing-starter:0.0.1-SNAPSHOT")
    // Optional, when using the PostgreSQL JDBC-backed event store:
    // implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Optional, when applying the bundled PostgreSQL schema through Flyway:
    // implementation("org.springframework.boot:spring-boot-starter-flyway")
}
```

### Configuration

```yaml
kopring:
  bricks:
    event-sourcing:
      enabled: true
      jdbc:
        table-name: event_store
        dialect: auto
        flyway:
          enabled: false
```

### PostgreSQL JDBC Storage

`event-sourcing-starter`는 JDBC starter를 끌고 오지 않습니다. PostgreSQL 저장소를 사용하려면 애플리케이션이 `spring-boot-starter-jdbc` 또는 `vt-jdbc-client-starter`처럼 `JdbcClient`를 제공하는 의존성을 별도로 추가해야 합니다.

`jdbc.dialect=auto`는 `spring.datasource.url`, `spring.datasource.jdbc-url`, `spring.datasource.hikari.jdbc-url`이 `jdbc:postgresql:`일 때만 JDBC 저장소를 켭니다. 커스텀 `DataSource`처럼 URL 감지가 어려운 경우에는 `kopring.bricks.event-sourcing.jdbc.dialect=postgresql`을 명시하세요.

PostgreSQL 테이블 예시는 `META-INF/kopring-bricks/event-sourcing/schema-postgresql.sql`에 포함되어 있습니다. starter가 운영 DB에 DDL을 기본으로 자동 실행하지는 않습니다.

Flyway를 쓰는 애플리케이션은 `spring-boot-starter-flyway`를 추가한 뒤 `kopring.bricks.event-sourcing.jdbc.flyway.enabled=true`로 bundled repeatable migration location을 추가할 수 있습니다. 이 opt-in은 기본 테이블명 `event_store`에서만 동작합니다. 테이블명을 바꾼 경우에는 아래 스키마를 애플리케이션 migration으로 복사해 조정하세요.

```yaml
kopring:
  bricks:
    event-sourcing:
      jdbc:
        flyway:
          enabled: true
```

Liquibase를 쓰거나 migration을 직접 관리하는 애플리케이션은 위 스키마를 애플리케이션 migration에 복사해 적용하세요. Spring resource로 직접 확인하려면 다음 경로를 사용합니다.

```kotlin
import org.springframework.core.io.ClassPathResource

val schema = ClassPathResource("META-INF/kopring-bricks/event-sourcing/schema-postgresql.sql")
```

Flyway location 상수도 제공합니다.

```kotlin
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EVENT_SOURCING_POSTGRESQL_FLYWAY_LOCATION
```

### EventStore Contract

`EventStore` 구현체는 다음 계약을 지켜야 합니다.

- `streamId`가 blank이면 `IllegalArgumentException` 발생
- `expectedVersion`이 0보다 작으면 `IllegalArgumentException` 발생
- append할 이벤트 목록이 비어 있으면 `IllegalArgumentException` 발생
- `fromVersion`이 1보다 작으면 `IllegalArgumentException` 발생
- append 성공 시 `expectedVersion + 1`부터 연속된 stream version 부여
- 현재 stream version이 `expectedVersion`과 다르면 `EventStreamVersionConflictException` 발생
- `load(streamId, fromVersion)`은 같은 stream의 `fromVersion` 이상 이벤트를 stream version 오름차순으로 반환

### Appending And Replaying Events

```kotlin
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventSourcingTemplate
import me.sensibile.kopringbricks.eventsourcing.autoconfigure.EventStoreEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoCommandService(
    private val events: EventSourcingTemplate,
) {
    @Transactional
    fun complete(todoId: String, expectedVersion: Long) {
        events.append(
            streamId = "todo-$todoId",
            expectedVersion = expectedVersion,
            events =
                listOf(
                    EventStoreEvent(
                        eventType = "todo.completed",
                        payloadJson = """{"id":"$todoId"}""",
                    ),
                ),
        )
    }

    fun history(todoId: String): List<String> =
        events.fold("todo-$todoId", emptyList()) { eventTypes, event ->
            eventTypes + event.eventType
        }
}
```

## Outbox

`outbox-starter`는 도메인 상태 변경과 외부 이벤트 발행 사이의 트랜잭션 간극을 줄이기 위한 transactional outbox 저장소 계약과 polling publisher 실행 흐름을 제공합니다. PostgreSQL datasource와 `JdbcClient`가 있으면 JDBC 저장소를 기본으로 구성하고, 그 외에는 logging 저장소로 내려갑니다.

기본 동작:

- `OutboxEventRepository` 자동 구성
- `OutboxEventAppender` 자동 구성
- 앱이 `OutboxEventPublisher`를 제공하면 `OutboxPollingService` 자동 구성
- `scheduler.enabled=true`이고 `OutboxPollingService`가 있으면 `OutboxScheduler` 자동 구성
- PostgreSQL datasource로 감지되면 `JdbcOutboxEventRepository` 구성
- 저장소가 없으면 `LoggingOutboxEventRepository` 구성
- 앱에서 `OutboxEventRepository` 또는 `OutboxScheduler` Bean을 등록하면 기본 구현 back off

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:outbox-starter:0.0.1-SNAPSHOT")
    // Optional, when using the PostgreSQL JDBC-backed repository:
    // implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Optional, when applying the bundled PostgreSQL schema through Flyway:
    // implementation("org.springframework.boot:spring-boot-starter-flyway")
}
```

### Configuration

```yaml
kopring:
  bricks:
    outbox:
      enabled: true
      jdbc:
        table-name: outbox_event
        dialect: auto
        flyway:
          enabled: false
      polling:
        claim-limit: 100
        claim-timeout: 5m
      retry:
        initial-delay: 5s
        max-delay: 5m
        multiplier: 2
      scheduler:
        enabled: false
        initial-delay: 0s
        fixed-delay: 1s
        pool-size: 1
        thread-name-prefix: kopring-bricks-outbox-
```

### PostgreSQL JDBC Storage

`outbox-starter`는 JDBC starter를 끌고 오지 않습니다. PostgreSQL 저장소를 사용하려면 애플리케이션이 `spring-boot-starter-jdbc` 또는 `vt-jdbc-client-starter`처럼 `JdbcClient`를 제공하는 의존성을 별도로 추가해야 합니다.

`jdbc.dialect=auto`는 `spring.datasource.url`, `spring.datasource.jdbc-url`, `spring.datasource.hikari.jdbc-url`이 `jdbc:postgresql:`일 때만 JDBC 저장소를 켭니다. 커스텀 `DataSource`처럼 URL 감지가 어려운 경우에는 `kopring.bricks.outbox.jdbc.dialect=postgresql`을 명시하세요.

PostgreSQL 테이블 예시는 `META-INF/kopring-bricks/outbox/schema-postgresql.sql`에 포함되어 있습니다. starter가 운영 DB에 DDL을 기본으로 자동 실행하지는 않습니다.

Flyway를 쓰는 애플리케이션은 `spring-boot-starter-flyway`를 추가한 뒤 `kopring.bricks.outbox.jdbc.flyway.enabled=true`로 bundled repeatable migration location을 추가할 수 있습니다. 이 opt-in은 기본 테이블명 `outbox_event`에서만 동작합니다. 테이블명을 바꾼 경우에는 스키마를 애플리케이션 migration으로 복사해 조정하세요.

```yaml
kopring:
  bricks:
    outbox:
      jdbc:
        flyway:
          enabled: true
```

Flyway location 상수도 제공합니다.

```kotlin
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OUTBOX_POSTGRESQL_FLYWAY_LOCATION
```

### Appending Events

```kotlin
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventAppender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeatureRuleService(
    private val rules: FeatureRuleRepository,
    private val outbox: OutboxEventAppender,
) {
    @Transactional
    fun enable(ruleId: String) {
        rules.enable(ruleId)
        outbox.append(
            OutboxEvent(
                aggregateType = "feature-rule",
                aggregateId = ruleId,
                eventType = "feature-rule.enabled",
                payloadJson = """{"enabled":true}""",
            ),
        )
    }
}
```

### Publishing Events

```kotlin
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEvent
import me.sensibile.kopringbricks.messaging.outbox.autoconfigure.OutboxEventPublisher
import org.springframework.stereotype.Component

@Component
class FeatureRuleOutboxPublisher : OutboxEventPublisher {
    override fun publish(event: OutboxEvent) {
        // Send to Kafka, SNS, webhook, or another application-specific transport.
    }
}
```

`OutboxPollingService`는 claim한 이벤트를 `OutboxEventPublisher`로 발행하고, 성공하면 published, 실패하면 retry 설정에 따라 다음 시도 시각을 계산해 failed로 기록합니다. `scheduler.enabled=true`를 설정하면 starter가 전용 `ThreadPoolTaskScheduler`로 polling loop를 실행합니다. 앱에서 `OutboxScheduler` Bean을 등록하면 기본 scheduler는 생성되지 않습니다.

## Audit Log

`audit-log-starter`는 애플리케이션의 관리 작업, 룰 변경, 설정 변경처럼 추적이 필요한 이벤트를 표준 `AuditEventPublisher` API로 남길 수 있게 합니다. `JdbcClient`가 있고 PostgreSQL datasource로 감지되면 JSONB 테이블에 저장하는 JDBC 저장소를 기본으로 구성하고, 그 외에는 logging 저장소로 내려갑니다.

기본 동작:

- `AuditEventPublisher`와 `AuditEventRepository` 자동 구성
- `JdbcClient`가 있고 PostgreSQL datasource로 감지되면 `JdbcAuditEventRepository` 구성
- 저장소가 없으면 `LoggingAuditEventRepository` 구성
- 앱에서 `AuditEventRepository` 또는 `AuditEventPublisher` Bean을 등록하면 기본 구현 back off
- 저장 실패는 기본적으로 애플리케이션 요청을 실패시키지 않음

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:audit-log-starter:0.0.1-SNAPSHOT")
    // Optional, when using the PostgreSQL JDBC-backed repository:
    // implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Optional, when applying the bundled PostgreSQL schema through Flyway:
    // implementation("org.springframework.boot:spring-boot-starter-flyway")
}
```

### Configuration

```yaml
kopring:
  bricks:
    audit-log:
      enabled: true
      publisher:
        fail-on-error: false
      jdbc:
        table-name: audit_log
        dialect: auto
        flyway:
          enabled: false
```

### PostgreSQL JDBC Storage

`audit-log-starter`는 logging-only 사용을 막지 않기 위해 JDBC starter를 끌고 오지 않습니다. PostgreSQL 저장소를 사용하려면 애플리케이션이 `spring-boot-starter-jdbc` 또는 `vt-jdbc-client-starter`처럼 `JdbcClient`를 제공하는 의존성을 별도로 추가해야 합니다.

`jdbc.dialect=auto`는 `spring.datasource.url`, `spring.datasource.jdbc-url`, `spring.datasource.hikari.jdbc-url`이 `jdbc:postgresql:`일 때만 JDBC 저장소를 켭니다. 커스텀 `DataSource`처럼 URL 감지가 어려운 경우에는 `kopring.bricks.audit-log.jdbc.dialect=postgresql`을 명시하세요.

PostgreSQL 테이블 예시는 `META-INF/kopring-bricks/audit-log/schema-postgresql.sql`에 포함되어 있습니다. starter가 운영 DB에 DDL을 기본으로 자동 실행하지는 않습니다.

Flyway를 쓰는 애플리케이션은 `spring-boot-starter-flyway`를 추가한 뒤 `kopring.bricks.audit-log.jdbc.flyway.enabled=true`로 bundled repeatable migration location을 추가할 수 있습니다. 이 opt-in은 기본 테이블명 `audit_log`에서만 동작합니다. 테이블명을 바꾼 경우에는 스키마를 애플리케이션 migration으로 복사해 조정하세요.

```yaml
kopring:
  bricks:
    audit-log:
      jdbc:
        flyway:
          enabled: true
```

Flyway location 상수도 제공합니다.

```kotlin
import me.sensibile.kopringbricks.auditlog.autoconfigure.AUDIT_LOG_POSTGRESQL_FLYWAY_LOCATION
```

### Publishing Events

```kotlin
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditActor
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEvent
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditEventPublisher
import me.sensibile.kopringbricks.auditlog.autoconfigure.AuditTarget
import org.springframework.stereotype.Service

@Service
class FeatureRuleService(
    private val auditEvents: AuditEventPublisher,
) {
    fun enableRule(ruleId: String, actorId: String) {
        auditEvents.publish(
            AuditEvent(
                actor = AuditActor(type = "user", id = actorId),
                action = "feature-rule.enabled",
                target = AuditTarget(type = "feature-rule", id = ruleId),
                afterStateJson = """{"enabled":true}""",
            ),
        )
    }
}
```

## Logging Observation

`logging-observation-starter`는 운영 환경에서 바로 검색 가능한 structured JSON logging과 요청 correlation context를 제공합니다. Spring Boot 4의 structured logging 설정을 사용하므로 별도 JSON encoder 의존성을 추가하지 않습니다.

기본 동작:

- `logging.structured.format.console=ecs` 기본값 적용
- incoming HTTP 요청의 `X-Request-Id` 추출
- request id가 없으면 생성
- response `X-Request-Id` header 설정
- MDC `request_id`에 request id 저장
- `RestClient` outbound 요청에 `X-Request-Id` 전파
- async executor용 MDC `TaskDecorator` 제공

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:logging-observation-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    logging-observation:
      enabled: true
      json:
        enabled: true
        console-format: ecs
      correlation:
        enabled: true
        request-header-name: X-Request-Id
        response-header-name: X-Request-Id
        generate-if-missing: true
      mdc:
        enabled: true
        request-id-key: request_id
      rest-client:
        propagation-enabled: true
      task-decorator:
        enabled: true
```

If `logging.structured.format.console` is already configured by the application, the starter leaves it unchanged.

## Micrometer Tracing

`micrometer-tracing-starter`는 Spring Boot 4 OpenTelemetry starter를 기반으로 Micrometer tracing과 OTLP export 설정을 제공합니다. `logging-observation-starter`를 함께 가져오므로 structured logging과 request correlation도 같이 활성화됩니다.

기본 동작:

- `spring-boot-starter-actuator` 추가
- `spring-boot-starter-opentelemetry` 추가
- `management.tracing.enabled=true` 기본값 적용
- `management.tracing.sampling.probability=1.0` 기본값 적용
- OTLP traces/metrics/logs endpoint는 명시한 경우에만 설정
- async/VT 경계에서 trace context를 전파하는 `ContextPropagatingTaskDecorator` 제공

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:micrometer-tracing-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    micrometer-tracing:
      enabled: true
      sampling:
        probability: 1.0
      otlp:
        traces-endpoint: http://localhost:4318/v1/traces
        metrics-endpoint: http://localhost:4318/v1/metrics
      context-propagation:
        task-decorator-enabled: true
```

If an application already configures `management.tracing.*`, `management.opentelemetry.*`, or `management.otlp.*` properties, this starter leaves those values unchanged.

## Web Error Handling

`problem-details-starter`는 Spring `ProblemDetail` 기반 에러 응답을 만들기 위한 공통 API를 제공합니다. `webmvc-error-starter`는 Spring Web MVC에서 예외를 `ProblemDetail` 응답으로 정규화합니다.

기본 동작:

- `spring.mvc.problemdetails.enabled=true` 기본값 적용
- `ApiException` 제공
- `ProblemDetailFactory` 제공
- validation 실패 응답에 `violations` 확장 필드 포함
- 모든 `ProblemDetail`에 `code` 확장 필드 포함
- MDC에 `request_id`가 있으면 응답에도 `request_id` 포함
- 500 응답은 기본적으로 내부 exception message를 숨김

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:webmvc-error-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    problem-details:
      enabled: true
      type-base-uri: https://sensibile.github.io/kopring-bricks/problems
      code-property-name: code
      request-id-property-name: request_id
    webmvc-error:
      enabled: true
      include-exception-message: false
      internal-error-code: INTERNAL_SERVER_ERROR
      validation-error-code: VALIDATION_FAILED
      request-id-mdc-key: request_id
```

### ApiException

```kotlin
import me.sensibile.kopringbricks.web.problem.autoconfigure.ApiException
import org.springframework.http.HttpStatus

class UserNotFoundException(userId: Long) : ApiException(
    status = HttpStatus.NOT_FOUND,
    code = "USER_NOT_FOUND",
    detail = "User $userId was not found",
    title = "User not found",
)
```

Example response:

```json
{
  "type": "https://sensibile.github.io/kopring-bricks/problems/user-not-found",
  "title": "User not found",
  "status": 404,
  "detail": "User 1 was not found",
  "code": "USER_NOT_FOUND",
  "request_id": "req-1"
}
```

## Caffeine Cache

`caffeine-cache-starter`는 Spring Cache 추상화에 Caffeine 기반 `CacheManager`를 제공합니다. 기본 spec은 local cache 운영에서 무난한 크기 제한, TTL, stats 기록을 켭니다.

기본 동작:

- `spring.cache.type=caffeine` 기본값 적용
- `@EnableCaching` 적용
- `CaffeineCacheManager` 제공
- 기본 spec: `maximumSize=10000,expireAfterWrite=10m,recordStats`
- cache name 목록 지정 가능
- cache별 Caffeine spec override 가능
- 사용자가 직접 `CacheManager` bean을 제공하면 물러남

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:caffeine-cache-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    caffeine-cache:
      enabled: true
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats
      cache-names:
        - users
        - products
      allow-null-values: false
      caches:
        users:
          spec: maximumSize=5000,expireAfterWrite=5m,recordStats
        products:
          spec: maximumSize=20000,expireAfterWrite=30m,recordStats
```

### Usage

```kotlin
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

data class User(
    val id: Long,
    val name: String,
)

interface UserRepository {
    fun findById(userId: Long): User
}

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    @Cacheable("users")
    fun findUser(userId: Long): User = userRepository.findById(userId)
}
```

## Resilience4j

`resilience4j-starter`는 Spring Boot 4용 Resilience4j starter, AspectJ AOP, Actuator, Micrometer metrics를 묶고 운영에서 무난하게 시작할 수 있는 기본 fault tolerance 설정을 제공합니다.

기본 동작:

- `resilience4j-spring-boot4` 추가
- `spring-boot-starter-aspectj` 추가
- `spring-boot-starter-actuator` 추가
- `resilience4j-micrometer` 추가
- circuit breaker 기본 config 적용
- retry 기본 config 적용
- time limiter 기본 config 적용
- semaphore bulkhead 기본 config 적용
- rate limiter 기본 config 적용
- circuit breaker/rate limiter health indicator 활성화
- 애플리케이션이 직접 설정한 `resilience4j.*` 값은 유지

### Installation

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/sensibile/kopring-bricks")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("me.sensibile:resilience4j-starter:0.0.1-SNAPSHOT")
}
```

### Configuration

```yaml
kopring:
  bricks:
    resilience4j:
      enabled: true
      circuit-breaker:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 100
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 10
        automatic-transition-from-open-to-half-open-enabled: true
      retry:
        max-attempts: 3
        wait-duration: 200ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2.0
      time-limiter:
        timeout-duration: 2s
        cancel-running-future: true
      bulkhead:
        max-concurrent-calls: 25
        max-wait-duration: 0ms
      rate-limiter:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0ms
      health:
        circuit-breakers-enabled: true
        rate-limiters-enabled: true
```

Generated Resilience4j defaults can still be overridden directly:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      github:
        base-config: default
        sliding-window-size: 50
  retry:
    instances:
      github:
        base-config: default
        max-attempts: 2
```

### Usage

```kotlin
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Service

@Service
class GithubService(
    private val githubApi: GithubApi,
) {
    @Retry(name = "github")
    @CircuitBreaker(name = "github", fallbackMethod = "fallback")
    fun findUser(login: String): GithubUser =
        githubApi.findUser(login)

    private fun fallback(login: String, ex: Throwable): GithubUser =
        GithubUser(login = login, displayName = "unknown")
}
```

## Build

```bash
./gradlew test
```

## Samples

`samples:todo-api` is a small Spring Boot Todo API that applies the starters in a consumer application. It covers Web MVC error handling, caching, RestClient defaults, audit events, optimistic concurrency through ETags, and outbox event recording/publishing.

```bash
./gradlew :samples:todo-api:test
```

## Test Support

`kopring-bricks-test-support` provides small test doubles for application tests that consume the starters:

- `RecordingAuditEventPublisher`
- `InMemoryEventStore`
- `InMemoryOutboxEventRepository`
- `RecordingOutboxEventPublisher`

```kotlin
dependencies {
    testImplementation("me.sensibile:kopring-bricks-test-support:0.0.1-SNAPSHOT")
}
```

## Development Setup

This project uses `mise` for the JDK and detekt CLI, and Homebrew for ktlint.

```bash
scripts/bootstrap-dev.sh
```

Useful local tasks:

```bash
mise run test
mise run lint:ktlint
mise run format:ktlint
mise run lint:detekt
mise run lint
mise run docs:check
mise run tooling:test
```

`format:ktlint` formats only changed Kotlin files by default.

To enable the repository pre-commit hook:

```bash
scripts/bootstrap-dev.sh --install-hooks
```

The hook runs `mise run lint`, which checks changed Kotlin files with ktlint and runs detekt.

Create a new starter module pair:

```bash
scripts/new-starter.sh messaging kafka-producer \
  --package-segment messaging.kafka \
  --class-prefix KafkaProducer \
  --display-name "Kafka Producer" \
  --description "Kafka producer defaults for Spring applications"
```

Use `--dry-run` to preview the generated files and `settings.gradle.kts` includes.

Regenerate project facts for AI-assisted README updates:

```bash
scripts/docs-facts.sh
scripts/docs-facts.sh --check
scripts/docs-coverage-check.sh
```

Use `docs/generated/project-facts.md` as factual source material and `docs/prompts/update-readme.md` as the update prompt. `mise run docs:check` verifies generated facts and checks that starter/test-support modules are mentioned in README and the application agent guide.

## Publishing

See [Release Guide](docs/release.md) for the GitHub Packages release flow.

Local verification:

```bash
./gradlew test publishToMavenLocal
```

GitHub Packages publishing is configured through Gradle `maven-publish`. In GitHub Actions, `GITHUB_ACTOR` and `GITHUB_TOKEN` are used automatically.

The included workflow publishes packages on `main` pushes, `v*` tag pushes, and manual workflow runs. Pull requests only run tests.

## License

This project is licensed under the MIT License.
