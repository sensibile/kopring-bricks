plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(project(":audit:audit-log-autoconfigure"))
    api(project(":messaging:outbox-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Test Support"
                description = "Test helpers for applications that consume Kopring Bricks starters."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
