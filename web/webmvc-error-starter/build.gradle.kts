plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":web:webmvc-error-autoconfigure"))
    api(libs.spring.boot.starter.webmvc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks WebMvc Error Starter"
                description = "Spring Boot starter for opinionated Web MVC ProblemDetail exception handling."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
