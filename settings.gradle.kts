pluginManagement {
    includeBuild("build-logic")
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
include("web:webmvc-error-autoconfigure")
include("web:webmvc-error-starter")
include("cache:caffeine-cache-autoconfigure")
include("cache:caffeine-cache-starter")
include("resilience:resilience4j-autoconfigure")
include("resilience:resilience4j-starter")
