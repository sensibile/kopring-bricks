plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation(project(":cache:caffeine-cache-starter"))
    implementation(project(":http-client:vt-rest-client-starter"))
    implementation(project(":observability:logging-observation-starter"))
    implementation(project(":resilience:resilience4j-starter"))
    implementation(project(":web:webmvc-error-starter"))

    implementation(libs.spring.boot.starter.validation)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}
