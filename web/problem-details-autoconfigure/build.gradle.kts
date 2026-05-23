plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(libs.spring.boot)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.web)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Problem Details Autoconfigure"
                description = "ProblemDetail support primitives and defaults for Spring applications."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
