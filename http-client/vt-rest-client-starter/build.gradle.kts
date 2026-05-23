plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":http-client:vt-rest-client-autoconfigure"))
    api(libs.spring.boot.starter.restclient)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks VT RestClient Starter"
                description = "Spring Boot starter for RestClient backed by JDK HttpClient and virtual-thread executor support."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
