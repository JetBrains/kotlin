import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js {
        binaries.executable()
        browser {

        }
    }
}

tasks.withType(KotlinJsIrLink::class.java) {
    this.compilerOptions.freeCompilerArgs.add("-Xir-per-file")
    this.compilerOptions.moduleKind.set(JsModuleKind.MODULE_ES)
}
