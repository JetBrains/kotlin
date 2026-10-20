/*
 * TODO(KT-XXXXX): Remove this plugin when the bootstrap KGP componentizes WASI test binaries.
 *  The stdlib imports `wasi:*` 0.2 interfaces, and a host gives these only to components. So this plugin makes
 *  `wasmWasiWasmtimeTest` run a component that [org.jetbrains.kotlin.build.wasi.ComponentizeWasiTestBinary] builds,
 *  not the core module.
 */

import org.jetbrains.kotlin.build.wasi.ComponentizeWasiTestBinary
import org.jetbrains.kotlin.build.wasi.WasmtimeComponentTestFramework
import org.jetbrains.kotlin.build.wasmtools.WasmToolsExtension
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

apply(plugin = "wasm-tools-configuration")
val wasmToolsKotlinBuild = the<WasmToolsExtension>()

val wasiTestTaskName = "wasmWasiWasmtimeTest"

val componentizeWasiTestBinary = tasks.register<ComponentizeWasiTestBinary>("componentizeWasmWasiTestBinary") {
    coreModule.set(tasks.named<KotlinJsTest>(wasiTestTaskName).flatMap { it.inputFileProperty })
    stdlibWit.set(project.isolated.rootProject.projectDirectory.dir("libraries/stdlib/wasm/wasi/internal/wit"))
    wasmTools.fileProvider(wasmToolsKotlinBuild.wasmToolsExecutable)
    component.set(layout.buildDirectory.file("wasiTestComponent/test.wasm"))
}

tasks.withType<KotlinJsTest>().matching { it.name == wasiTestTaskName }.configureEach {
    dependsOn(componentizeWasiTestBinary)
    val component = componentizeWasiTestBinary.flatMap { it.component }
    doFirst {
        this as KotlinJsTest
        testFramework = WasmtimeComponentTestFramework(testFramework!!, component.get().asFile)
    }
}
