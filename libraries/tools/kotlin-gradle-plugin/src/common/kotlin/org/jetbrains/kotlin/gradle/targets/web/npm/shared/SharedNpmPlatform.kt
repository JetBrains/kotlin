/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

/**
 * Describes one of the two independent shared-npm-project flows: Kotlin/JS and Kotlin/WasmJS
 * use separate Configurations, tasks, and reports, differing only by the data carried here.
 */
internal enum class SharedNpmPlatform(
    val targetId: String,
    val suffix: String,
    val setupTaskName: String,
    val rootDirName: String,
) {
    JS(
        targetId = "js",
        suffix = "Js",
        setupTaskName = "kotlinSetupSharedNpmProjectJs",
        rootDirName = "js",
    ),
    WASM_JS(
        targetId = "wasmJs",
        suffix = "WasmJs",
        setupTaskName = "kotlinSetupSharedNpmProjectWasmJs",
        rootDirName = "wasm",
    ),
    ;

    val reportConfigurationName: String get() = "kotlinNpmDependenciesReport$suffix"

    val sharedDependenciesConsumableConfigurationName: String get() = "kotlinNpmSharedDependencies$suffix"

    val sharedDependenciesResolvableConfigurationName: String get() = "kotlinNpmSharedDependencies${suffix}Resolver"
}
