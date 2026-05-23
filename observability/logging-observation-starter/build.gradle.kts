plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":observability:logging-observation-autoconfigure"))
    api(libs.spring.boot.starter.logging)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Logging Observation Starter"
                description = "Spring Boot starter for structured logging, request correlation, MDC propagation, and RestClient correlation propagation."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
