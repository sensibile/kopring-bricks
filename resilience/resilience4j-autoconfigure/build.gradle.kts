plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Resilience4j Autoconfigure"
                description = "Opinionated Resilience4j defaults for Spring Boot applications."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
