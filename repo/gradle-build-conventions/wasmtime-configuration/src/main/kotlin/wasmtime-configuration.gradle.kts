@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.build.wasmtime.WasmtimeExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimeEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimePlugin

if (project == rootProject) {
    error("${project.path} is the root project, apply wasmtime-root-configuration instead of wasmtime-configuration")
}

project.plugins.apply(WasmtimePlugin::class.java)
val wasmtimeEnvSpec = project.the<WasmtimeEnvSpec>().apply {
    downloadBaseUrl.set(null as String?)
}

val wasmtimeKotlinBuild = extensions.create<WasmtimeExtension>(
    "wasmtimeKotlinBuild",
    project,
    wasmtimeEnvSpec,
)

with(wasmtimeKotlinBuild) {
    wasmtimeEnvSpec.version.set(wasmtimeVersion)
}
