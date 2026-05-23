plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":observability:logging-observation-starter"))
    api(project(":observability:micrometer-tracing-autoconfigure"))
    api(libs.spring.boot.starter.actuator)
    api(libs.spring.boot.starter.opentelemetry)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Micrometer Tracing Starter"
                description = "Spring Boot starter for Micrometer Tracing with OpenTelemetry defaults."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
