# Application Agent Guide

This guide is for agents building applications that consume `kopring-bricks`.
Use it to decide how to add starters, verify behavior, and report required library changes.

## When To Use This Library

Use `kopring-bricks` when an application needs one of the existing opinionated Spring Boot starters:

- `vt-rest-client-starter` for JDK `HttpClient` backed `RestClient` defaults and named clients.
- `vt-jdbc-client-starter` for `JdbcClient` plus virtual-thread helper operations.
- `logging-observation-starter` for structured logging, request correlation, and outbound `RestClient` propagation.
- `micrometer-tracing-starter` for Micrometer Tracing and OpenTelemetry defaults.
- `problem-details-starter` for Spring `ProblemDetail` primitives and defaults.
- `webmvc-error-starter` for opinionated Web MVC error handling.
- `caffeine-cache-starter` for Caffeine cache defaults.
- `resilience4j-starter` for Resilience4j defaults.
- `audit-log-starter` for audit event publishing and JDBC-backed audit log storage.

Prefer starter modules in applications. Autoconfigure modules are library internals unless an application has a specific reason to depend on them directly.

## App Integration Checklist

1. Add the GitHub Packages repository and credentials.
2. Add only the starter dependencies the app actually uses.
3. Configure `kopring.bricks.*` properties in application configuration.
4. Write at least one application-level integration test for the behavior the app relies on.
5. If the app reveals a missing library capability, open an issue in `kopring-bricks` instead of patching around it in the app.
6. Use `audit-log-starter` for admin actions, configuration changes, rule changes, approval decisions, and other events that need an operator-visible trail.

## Gradle Example

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
    implementation("me.sensibile:webmvc-error-starter:0.0.1-SNAPSHOT")
}
```

## When To Open A Library Issue

Open an issue in `kopring-bricks` when the application needs behavior that should be reusable across apps:

- A starter is missing a configuration property.
- Auto-configuration order or conditions are wrong.
- A default is unsafe, surprising, or incompatible with Spring Boot conventions.
- A feature requires app-specific boilerplate that belongs in a starter.
- Dependency, Gradle, Kotlin, Java, or Spring Boot compatibility blocks the app.
- Documentation is not enough for an app agent to use the starter correctly.

Do not open a library issue for app-local domain logic, app-specific beans, credentials, deployment settings, or one-off business behavior.

## Issue Template

```markdown
## Context
- Application:
- Starter/module:
- Spring Boot version:
- Java/Kotlin/Gradle versions:

## Problem
Describe what the application needs and what currently fails or requires boilerplate.

## Expected Library Behavior
Describe the starter API, configuration property, or auto-configuration behavior the app should be able to rely on.

## App-Level Workaround
Describe any temporary workaround currently used in the application.

## Acceptance Criteria
- [ ] Library behavior is implemented or fixed.
- [ ] Tests cover the scenario.
- [ ] Sample or documentation is updated when useful.
```

## Handoff Workflow

When an application task discovers a library change:

1. Create or update an issue in `kopring-bricks`.
2. Reference the issue from the application task.
3. Keep the app workaround small and explicit.
4. Implement the library change in a separate `kopring-bricks` branch and PR.
5. Publish or consume the updated library version before removing the app workaround.

Use `Closes #<issue-number>` only when the PR fully resolves the issue. Use `Refs #<issue-number>` when the PR is partial or exploratory.

## Agent Notes

- Read `README.md` first for starter-specific usage and properties.
- Use `docs/generated/project-facts.md` as generated factual context when updating docs.
- Do not copy internal auto-configuration code into an application.
- Do not add app-specific conditions to a reusable starter without an issue that explains the broader use case.
- Prefer library tests that use `ApplicationContextRunner` for auto-configuration behavior.
