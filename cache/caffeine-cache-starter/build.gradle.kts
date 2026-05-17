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
    api(project(":cache:caffeine-cache-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-cache")
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
                name = "Kopring Bricks Caffeine Cache Starter"
                description = "Spring Boot starter for opinionated Caffeine cache defaults."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
