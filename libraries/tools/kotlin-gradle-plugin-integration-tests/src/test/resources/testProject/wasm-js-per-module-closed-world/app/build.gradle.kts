plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    wasmJs {
        binaries.executable().forEach {
            it.linkTask.configure {
                compilerOptions.freeCompilerArgs.add("-Xwasm-generate-closed-world-multimodule")
            }
        }
        d8 {}
    }
    sourceSets {
        wasmJsMain {
            dependencies {
                implementation(project(":mid"))
            }
        }
    }
}
