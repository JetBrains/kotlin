import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

plugins {
    kotlin("multiplatform")
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
    }
}
