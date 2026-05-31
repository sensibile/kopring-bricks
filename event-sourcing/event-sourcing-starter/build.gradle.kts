plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":event-sourcing:event-sourcing-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Event Sourcing Starter"
                description = "Spring Boot starter for thin event sourcing storage and replay primitives."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
