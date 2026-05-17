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
```

`*-autoconfigure` 모듈은 실제 auto-configuration을 제공하고, `*-starter` 모듈은 애플리케이션에서 가져다 쓰는 starter 의존성입니다.

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

## Build

```bash
./gradlew test
```

## Publishing

Local verification:

```bash
./gradlew test publishToMavenLocal
```

GitHub Packages publishing is configured through Gradle `maven-publish`. In GitHub Actions, `GITHUB_ACTOR` and `GITHUB_TOKEN` are used automatically.

The included workflow publishes packages on `main` pushes, `v*` tag pushes, and manual workflow runs. Pull requests only run tests.

## License

This project is licensed under the MIT License.
