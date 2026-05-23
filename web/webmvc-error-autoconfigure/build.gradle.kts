plugins {
    id("kopring.kotlin-autoconfigure-conventions")
}

dependencies {
    api(project(":web:problem-details-autoconfigure"))
    api(libs.spring.boot.webmvc)
    api(libs.spring.webmvc)
    api(libs.slf4j.api)

    testImplementation(libs.spring.boot.starter.webmvc.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks WebMvc Error Autoconfigure"
                description = "Opinionated Web MVC ProblemDetail exception handling and error attributes."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
