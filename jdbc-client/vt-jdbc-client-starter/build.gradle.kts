plugins {
    id("kopring.starter-conventions")
}

dependencies {
    api(project(":jdbc-client:vt-jdbc-client-autoconfigure"))
    api(libs.spring.boot.starter.jdbc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name = "Kopring Bricks VT JdbcClient Starter"
                description = "Spring Boot starter for JdbcClient with virtual-thread executor support."
                url = "https://github.com/sensibile/kopring-bricks"
            }
        }
    }
}
