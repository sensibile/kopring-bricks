plugins {
    id("kopring.java-library-conventions")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(libs.findLibrary("spring-boot-starter-test").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}
