import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js()
    wasmJs()
    macosArm64()

    publishing {
        publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
    }
}
