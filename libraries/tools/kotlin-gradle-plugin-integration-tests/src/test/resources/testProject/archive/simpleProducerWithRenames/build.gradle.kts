import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm("renamedJvm")
    js("renamedJs")
    wasmJs("renamedWasmJs")
    macosArm64("renamedMacosArm64")

    publishing {
        publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
    }

}
