plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.jdbc)
    api(libs.spring.jdbc)

    testImplementation(libs.spring.boot.starter.jdbc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks VT JdbcClient Autoconfigure"
                description = "Auto-configuration for Spring JdbcClient with virtual-thread executor support."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
