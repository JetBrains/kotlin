plugins {
    `kotlin-dsl`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(kotlin("test-junit5"))
}

kotlin.jvmToolchain(8)

tasks {
    test {
        useJUnitPlatform()
    }
}

gradlePlugin {
    plugins {
        create("internal-gradle-setup") {
            id = "internal-gradle-setup"
            implementationClass = "org.jetbrains.kotlin.build.InternalGradleSetupSettingsPlugin"
        }
    }
}