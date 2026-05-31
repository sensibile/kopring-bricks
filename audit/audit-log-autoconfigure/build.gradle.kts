plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    implementation(project(":support:jdbc-autoconfigure"))

    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.jdbc)
    api(libs.spring.jdbc)
    api(libs.slf4j.api)

    testImplementation(libs.spring.boot.starter.jdbc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Audit Log Autoconfigure"
                description = "Auto-configuration for audit event publishing and JDBC-backed audit log storage."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
