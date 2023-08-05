/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink

private fun Project.createBuiltInsSources() = tasks.register("builtInsSources", Sync::class.java) {
    val unimplementedNativeBuiltIns =
        (file("$rootDir/core/builtins/native/kotlin/").list().toSortedSet() - file("$rootDir/libraries/stdlib/wasm/builtins/kotlin/").list())
            .map { "core/builtins/native/kotlin/$it" }

    val sources = listOf(
        "core/builtins/src/kotlin/"
    ) + unimplementedNativeBuiltIns

    val excluded = listOf(
        // JS-specific optimized version of emptyArray() already defined
        "ArrayIntrinsics.kt",
        // Included with K/N collections
        "Collections.kt", "Iterator.kt", "Iterators.kt"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            excluded.forEach {
                exclude(it)
            }
        }
    }

    into("$buildDir/builtInsSources")
}

private fun Project.createCommonMainSources() = tasks.register("commonMainSources", Sync::class.java) {
    val sources = listOf(
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/",
        "libraries/stdlib/wasm/builtins/",
        "libraries/stdlib/wasm/internal/",
        "libraries/stdlib/wasm/runtime/",
        "libraries/stdlib/wasm/src/",
        "libraries/stdlib/wasm/stubs/",
        "libraries/stdlib/native-wasm/src/",
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
        }
    }

    into("$buildDir/commonMainSources")

    dependsOn(":prepare:build.version:writeStdlibVersion")
}

private fun Project.createCommonTestSources() = tasks.register("commonTestSources", Sync::class.java) {
    val sources = listOf(
        "libraries/stdlib/test/",
        "libraries/stdlib/common/test/",
        "libraries/stdlib/wasm/test/",
        "libraries/stdlib/native-wasm/test/",
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            // exclusions due to KT-51647
            exclude("generated/minmax")
            exclude("collections/MapTest.kt")
        }
    }

    into("$buildDir/commonTestSources")
}

fun Project.configureWasmStdLib(
    wasmTargetParameter: String,
    wasmTargetAttribute: KotlinWasmTargetAttribute,
    targetDependentSources: List<String>,
    targetDependentTestSources: List<String>,
    kotlinTestDependencyName: String,
    withKotlinMPP: (KotlinMultiplatformExtension.() -> Unit) -> Unit
) {

    val builtInsSources = createBuiltInsSources()
    val commonMainSources = createCommonMainSources()
    val commonTestSources = createCommonTestSources()

    withKotlinMPP {
        @Suppress("DEPRECATION")
        @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
        wasm("wasm") {
            nodejs()
            attributes {
                attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, wasmTargetAttribute)
            }
        }

        sourceSets.named("commonMain") {
            kotlin.srcDirs(files(commonMainSources.map { it.destinationDir }))
            kotlin.srcDirs(files(builtInsSources.map { it.destinationDir }))
        }

        sourceSets.named("wasmMain") {
            kotlin.srcDirs(targetDependentSources)
        }

        sourceSets.named("commonTest") {
            dependencies {
                api(project(kotlinTestDependencyName))
            }
            kotlin.srcDir(files(commonTestSources.map { it.destinationDir }))
        }

        sourceSets.named("wasmTest") {
            dependencies {
                api(project(kotlinTestDependencyName))
            }
            kotlin.srcDirs(targetDependentTestSources)
        }
    }

    tasks.named("compileKotlinWasm", KotlinCompile::class.java) {
        // TODO: enable explicit API mode
        kotlinOptions.allWarningsAsErrors = true
        kotlinOptions.freeCompilerArgs += "-Xir-module-name=kotlin"
        dependsOn(commonMainSources)
        dependsOn(builtInsSources)
    }

    tasks.named("compileTestKotlinWasm", KotlinCompile::class.java) {
        // TODO: fix all warnings, enable and -Werror
        kotlinOptions.suppressWarnings = true
    }

    tasks.withType<KotlinCompile<*>>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-opt-in=kotlin.ExperimentalMultiplatform",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.io.encoding.ExperimentalEncodingApi",
            "-opt-in=kotlin.wasm.unsafe.UnsafeWasmMemoryApi",
            "-Xwasm-target=$wasmTargetParameter"
        )
    }

    tasks.named("compileTestDevelopmentExecutableKotlinWasm", KotlinJsIrLink::class.java) {
        kotlinOptions.freeCompilerArgs += listOf("-Xwasm-enable-array-range-checks")
    }
}