plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))

    commonCompileOnly(project(":kotlin-gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("kotlinWasmWasiRuntimePlugin") {
            id = "org.jetbrains.kotlin.plugin.wasm.wasi.runtime"
            displayName = "Kotlin Wasm WASI Runtime plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.targets.wasm.runtime.WasmWasiRuntimeGradlePlugin"
        }
    }
}
