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
    api(project(":web:problem-details-autoconfigure"))
    api("org.springframework.boot:spring-boot-webmvc")
    api("org.springframework:spring-webmvc")
    api("org.slf4j:slf4j-api")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
                name = "Kopring Bricks WebMvc Error Autoconfigure"
                description = "Opinionated Web MVC ProblemDetail exception handling and error attributes."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
