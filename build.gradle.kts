plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

group = "me.sensibile"
version = "0.0.1-SNAPSHOT"
description = "kopring-bricks"

subprojects {
    group = rootProject.group
    version = rootProject.version
    description = rootProject.description

    repositories {
        mavenCentral()
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(24)
            }
        }

        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "GitHubPackages"
                    val githubRepository = System.getenv("GITHUB_REPOSITORY") ?: "sensibile/kopring-bricks"
                    url = uri("https://maven.pkg.github.com/$githubRepository")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}
