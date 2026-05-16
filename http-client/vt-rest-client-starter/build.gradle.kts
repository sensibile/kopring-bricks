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
    api(project(":http-client:vt-rest-client-autoconfigure"))
    api("org.springframework.boot:spring-boot-starter-restclient")
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
                name = "Kopring Bricks VT RestClient Starter"
                description = "Spring Boot starter for RestClient backed by JDK HttpClient and virtual-thread executor support."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
