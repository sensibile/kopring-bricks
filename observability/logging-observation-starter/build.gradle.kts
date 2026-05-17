plugins {
    `java-library`
    `maven-publish`
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api(project(":observability:logging-observation-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-logging")
}

java {
    withSourcesJar()
    withJavadocJar()
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
