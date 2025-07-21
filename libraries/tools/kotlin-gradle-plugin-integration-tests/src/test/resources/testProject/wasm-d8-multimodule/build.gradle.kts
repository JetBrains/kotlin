@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        binaries.executable()
        d8 {
            runTask {
                val directory = this.inputFileProperty.get().asFile.parentFile
                inputFileProperty.set(File(directory, "app.mjs"))
            }
        }
        val multiModuleMode = if ("MULTIMODULE_MODE_MASTER_ENABLE".isEmpty()) "slave" else "master"
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-multimodule-mode=$multiModuleMode")
        }
    }
}

tasks.register<Copy>("masterCopy") {
    from(File(project.buildDir, "wasm"))
    into(File(project.buildDir, "master"))
}

tasks.register<Copy>("masterCopyBack") {
    from(File(project.buildDir, "master"))
    into(File(project.buildDir, "wasm"))
}