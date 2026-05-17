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
    api(project(":observability:logging-observation-starter"))
    api(project(":observability:micrometer-tracing-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-opentelemetry")
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
                name = "Kopring Bricks Micrometer Tracing Starter"
                description = "Spring Boot starter for Micrometer Tracing with OpenTelemetry defaults."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
