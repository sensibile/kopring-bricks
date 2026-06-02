plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    implementation(project(":support:jdbc-autoconfigure"))

    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.jdbc)
    api(libs.spring.jdbc)

    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(libs.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Event Sourcing Autoconfigure"
                description = "Auto-configuration for thin event sourcing storage and replay primitives."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
