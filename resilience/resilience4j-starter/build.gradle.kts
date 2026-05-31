plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":resilience:resilience4j-autoconfigure"))
    api(libs.spring.boot.starter.actuator)
    api(libs.spring.boot.starter.aspectj)
    api(libs.resilience4j.spring.boot4)
    api(libs.resilience4j.micrometer)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks Resilience4j Starter"
                description = "Starter dependencies and production-oriented defaults for Resilience4j."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
