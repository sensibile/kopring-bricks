pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "kopring-bricks"

include("http-client:vt-rest-client-autoconfigure")
include("http-client:vt-rest-client-starter")
include("jdbc-client:vt-jdbc-client-autoconfigure")
include("jdbc-client:vt-jdbc-client-starter")
include("observability:logging-observation-autoconfigure")
include("observability:logging-observation-starter")
include("observability:micrometer-tracing-autoconfigure")
include("observability:micrometer-tracing-starter")
include("web:problem-details-autoconfigure")
include("web:problem-details-starter")
include("web:concurrency-control-autoconfigure")
include("web:concurrency-control-starter")
include("web:webmvc-error-autoconfigure")
include("web:webmvc-error-starter")
include("cache:caffeine-cache-autoconfigure")
include("cache:caffeine-cache-starter")
include("resilience:resilience4j-autoconfigure")
include("resilience:resilience4j-starter")
include("audit:audit-log-autoconfigure")
include("audit:audit-log-starter")
include("event-sourcing:event-sourcing-autoconfigure")
include("event-sourcing:event-sourcing-starter")
include("messaging:outbox-autoconfigure")
include("messaging:outbox-starter")
include("test-support:kopring-bricks-test-support")
include("samples:todo-api")
