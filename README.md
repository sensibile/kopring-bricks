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
samples/
  todo-api
```

`*-autoconfigure` 모듈은 실제 auto-configuration을 제공하고, `*-starter` 모듈은 애플리케이션에서 가져다 쓰는 starter 의존성입니다.

애플리케이션 개발 에이전트가 이 라이브러리를 소비하거나 라이브러리 변경 요청을 넘겨야 할 때는 [Application Agent Guide](docs/application-agent-guide.md)를 참고하세요.

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

## Audit Log

`audit-log-starter`는 애플리케이션의 관리 작업, 룰 변경, 설정 변경처럼 추적이 필요한 이벤트를 표준 `AuditEventPublisher` API로 남길 수 있게 합니다. `JdbcClient`가 있으면 PostgreSQL JSONB 테이블에 저장하는 JDBC 저장소를 기본으로 구성하고, 없으면 logging 저장소로 내려갑니다.

기본 동작:

- `AuditEventPublisher`와 `AuditEventRepository` 자동 구성
- `JdbcClient`가 있으면 `JdbcAuditEventRepository` 구성
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
```

PostgreSQL 테이블 예시는 `META-INF/kopring-bricks/audit-log/schema-postgresql.sql`에 포함되어 있습니다. starter가 운영 DB에 DDL을 자동 실행하지는 않습니다.

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

@Service
class UserService {
    @Cacheable("users")
    fun findUser(userId: Long): User {
        TODO("load user")
    }
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

`samples:todo-api` is a small Spring Boot Todo API that applies the starters in a consumer application.

```bash
./gradlew :samples:todo-api:test
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
```

Use `docs/generated/project-facts.md` as factual source material and `docs/prompts/update-readme.md` as the update prompt.

## Publishing

Local verification:

```bash
./gradlew test publishToMavenLocal
```

GitHub Packages publishing is configured through Gradle `maven-publish`. In GitHub Actions, `GITHUB_ACTOR` and `GITHUB_TOKEN` are used automatically.

The included workflow publishes packages on `main` pushes, `v*` tag pushes, and manual workflow runs. Pull requests only run tests.

## License

This project is licensed under the MIT License.
