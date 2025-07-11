import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

kotlin {
    linuxX64()
    linuxArm64()
    iosX64()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main").cinterops.create("dep")
    }
}

publishing {
    repositories {
        maven {
            name = "build"
            url = uri("<localRepo>")
        }
    }
}

