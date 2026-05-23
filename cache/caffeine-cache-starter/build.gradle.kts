plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":cache:caffeine-cache-autoconfigure"))
    api(libs.spring.boot.starter.cache)
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
