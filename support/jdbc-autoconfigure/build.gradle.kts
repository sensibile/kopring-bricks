plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.jdbc)

    compileOnly(libs.flyway.core)

    testImplementation(libs.flyway.core)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks JDBC Autoconfigure Support"
                description = "Shared JDBC auto-configuration helpers for Kopring Bricks modules."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
