plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":audit:audit-log-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Audit Log Starter"
                description = "Spring Boot starter for audit event publishing and JDBC-backed audit log storage."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
