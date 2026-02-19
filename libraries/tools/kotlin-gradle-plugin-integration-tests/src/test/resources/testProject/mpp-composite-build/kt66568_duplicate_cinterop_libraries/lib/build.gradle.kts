import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvm()
    linuxX64 {
        compilations.getByName("main").cinterops.create("foo") {
            definitionFile.set(file("src/interop/foo.def"))
        }
    }
    linuxArm64 {
        compilations.getByName("main").cinterops.create("foo") {
            definitionFile.set(file("src/interop/foo.def"))
        }
    }
}

group = "test"
version = "1.0-SNAPSHOT"

publishing {
    repositories {
        maven("<localRepo>")
    }
}