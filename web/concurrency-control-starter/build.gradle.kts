plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":web:concurrency-control-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Concurrency Control Starter"
                description = "Spring Boot starter for ETag, If-Match, and idempotency conflict primitives."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
