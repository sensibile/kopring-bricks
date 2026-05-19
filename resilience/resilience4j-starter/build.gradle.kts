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
    api(project(":resilience:resilience4j-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-aspectj")
    api("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    api("io.github.resilience4j:resilience4j-micrometer:2.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
                name = "Kopring Bricks Resilience4j Starter"
                description = "Starter dependencies and production-oriented defaults for Resilience4j."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
