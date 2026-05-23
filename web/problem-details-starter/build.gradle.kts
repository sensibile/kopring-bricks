plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":web:problem-details-autoconfigure"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Problem Details Starter"
                description = "Spring Boot starter for ProblemDetail support primitives and defaults."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
