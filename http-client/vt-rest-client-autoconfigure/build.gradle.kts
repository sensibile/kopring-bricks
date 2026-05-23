plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.restclient)
    api(libs.spring.web)

    testImplementation(libs.spring.boot.starter.restclient)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks VT RestClient Autoconfigure"
                description = "Auto-configuration for Spring RestClient backed by JDK HttpClient and virtual-thread executor support."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
