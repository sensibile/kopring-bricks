plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api("org.springframework.boot:spring-boot")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-restclient")
    api("org.springframework:spring-web")
    api("org.slf4j:slf4j-api")

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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
                name = "Kopring Bricks Logging Observation Autoconfigure"
                description = "Auto-configuration for structured logging, request correlation, MDC propagation, and RestClient correlation propagation."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
