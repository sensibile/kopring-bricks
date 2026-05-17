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
    api(project(":web:webmvc-error-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-webmvc")
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
                name = "Kopring Bricks WebMvc Error Starter"
                description = "Spring Boot starter for opinionated Web MVC ProblemDetail exception handling."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
