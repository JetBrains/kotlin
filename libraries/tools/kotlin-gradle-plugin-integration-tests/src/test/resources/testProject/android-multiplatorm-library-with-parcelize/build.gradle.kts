import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

kotlin {
    androidLibrary {
        compileSdk = 31
        namespace = "org.jetbrains.kotlin.sample"
    }
    jvm()
}

pluginManager.withPlugin("kotlin-parcelize") {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=com.example.shared.Parcelize",
            )
        }
    }
}