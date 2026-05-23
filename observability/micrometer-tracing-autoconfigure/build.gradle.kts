plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.core)
    api(libs.context.propagation)

    compileOnly(project(":observability:logging-observation-autoconfigure"))

    testImplementation(project(":observability:logging-observation-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Micrometer Tracing Autoconfigure"
                description = "Auto-configuration defaults for Micrometer Tracing with OpenTelemetry and context propagation."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
