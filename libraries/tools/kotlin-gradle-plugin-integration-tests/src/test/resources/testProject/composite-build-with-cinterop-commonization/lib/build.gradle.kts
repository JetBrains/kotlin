import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "org.example"
version = "1.0"

fun KotlinNativeTarget.cinterop(name: String = "a") {
    compilations
        .getByName("main")
        .cinterops
        .create(name)
}

kotlin {
    targetHierarchy.default()

    jvm()

    linuxX64 {
        cinterop()
        cinterop("b")
    }
    linuxArm64 {
        cinterop()
        cinterop("c")
    }

    sourceSets {
    }
}
