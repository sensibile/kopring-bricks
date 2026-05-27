plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":messaging:outbox-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Outbox Starter"
                description = "Spring Boot starter for transactional outbox event storage."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
