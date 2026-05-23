plugins {
    id("kopring.java-library-conventions")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("kotlin-reflect").get())

    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
    testImplementation(libs.findLibrary("spring-boot-test").get())
    testImplementation(libs.findLibrary("spring-boot-test-autoconfigure").get())
    testImplementation(libs.findLibrary("kotlin-test-junit5").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
