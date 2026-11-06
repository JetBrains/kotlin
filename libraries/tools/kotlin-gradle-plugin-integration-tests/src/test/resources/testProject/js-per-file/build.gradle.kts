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
        compilerOptions {
            moduleKind.set(JsModuleKind.MODULE_ES)
        }
    }
}

tasks.withType(KotlinJsIrLink::class.java) {
    this.compilerOptions.freeCompilerArgs.add("-Xir-per-file")
}
