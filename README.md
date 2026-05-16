# kopring-bricks

Spring Boot 기반 Kotlin 애플리케이션에서 반복해서 쓰는 설정을 starter 형태로 제공하는 라이브러리 모음입니다.

## Modules

```text
http-client/
  vt-rest-client-autoconfigure
  vt-rest-client-starter
```

`vt-rest-client-autoconfigure`는 실제 auto-configuration을 제공하고, `vt-rest-client-starter`는 애플리케이션에서 가져다 쓰는 starter 의존성입니다.

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
