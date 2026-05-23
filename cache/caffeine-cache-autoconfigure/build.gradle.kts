plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.context.support)
    api(libs.caffeine)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Caffeine Cache Autoconfigure"
                description = "Opinionated Caffeine cache auto-configuration for Spring Cache."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
