plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(project(":web:problem-details-autoconfigure"))
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.web)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Concurrency Control Autoconfigure"
                description = "Auto-configuration for ETag, If-Match, and idempotency conflict primitives."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
