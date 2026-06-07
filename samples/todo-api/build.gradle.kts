plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kover)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation(project(":cache:caffeine-cache-starter"))
    implementation(project(":event-sourcing:event-sourcing-starter"))
    implementation(project(":http-client:vt-rest-client-starter"))
    implementation(project(":observability:logging-observation-starter"))
    implementation(project(":resilience:resilience4j-starter"))
    implementation(project(":audit:audit-log-starter"))
    implementation(project(":messaging:outbox-starter"))
    implementation(project(":web:concurrency-control-starter"))
    implementation(project(":web:webmvc-error-starter"))

    implementation(libs.jackson.databind)
    implementation(libs.spring.boot.starter.validation)

    testImplementation(project(":test-support:kopring-bricks-test-support"))

    // JDBC/Flyway dependencies are only used by TodoJdbcBricksApplicationTests.
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)
    testImplementation(libs.postgresql)
    testImplementation(libs.spring.boot.flyway)
    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
