plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
}

java {
    withSourcesJar()
    withJavadocJar()
}
