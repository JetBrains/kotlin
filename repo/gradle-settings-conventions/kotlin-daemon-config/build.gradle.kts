plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // TODO: migrate to api only dependency once Kotlin daemon configuration will be available there (Yahor)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
}

kotlin.jvmToolchain(8)
