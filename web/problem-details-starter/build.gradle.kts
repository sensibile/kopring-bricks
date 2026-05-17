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
    api(project(":web:problem-details-autoconfigure"))
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
                name = "Kopring Bricks Problem Details Starter"
                description = "Spring Boot starter for ProblemDetail support primitives and defaults."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
