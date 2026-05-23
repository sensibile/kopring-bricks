plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.restclient)
    api(libs.spring.web)
    api(libs.slf4j.api)

    compileOnly(libs.jakarta.servlet.api)

    testImplementation(libs.jakarta.servlet.api)
    testImplementation(libs.spring.boot.starter.webmvc.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Logging Observation Autoconfigure"
                description = "Auto-configuration for structured logging, request correlation, MDC propagation, and RestClient correlation propagation."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
