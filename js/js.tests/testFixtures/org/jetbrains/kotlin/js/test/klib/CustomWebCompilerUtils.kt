/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.klib.CustomCompiler
import org.jetbrains.kotlin.test.klib.CustomCompilerArtifacts
import java.io.File
import kotlin.test.fail

/**
 * An accessor to "custom" (alternative) Kotlin/JS compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customJsCompilerSettings: CustomWebCompilerSettings by lazy {
    createCustomWebCompilerSettings(
        compilerClasspathPropertyName = "kotlin.internal.js.test.compat.customCompilerClasspath",
        runtimeDependenciesPropertyName = "kotlin.internal.js.test.compat.runtimeDependencies",
        versionPropertyName = "kotlin.internal.js.test.compat.customCompilerVersion",
        stdlibArtifactName = "kotlin-stdlib-js",
        kotlinTestArtifactName = "kotlin-test-js",
    )
}

/**
 * An accessor to "custom" (alternative) Kotlin/Wasm compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
val customWasmJsCompilerSettings: CustomWebCompilerSettings by lazy {
    createCustomWebCompilerSettings(
        compilerClasspathPropertyName = "kotlin.internal.wasm.test.compat.customCompilerClasspath",
        runtimeDependenciesPropertyName = "kotlin.internal.wasm.test.compat.runtimeDependencies",
        versionPropertyName = "kotlin.internal.wasm.test.compat.customCompilerVersion",
        stdlibArtifactName = "kotlin-stdlib-wasm-js",
        kotlinTestArtifactName = "kotlin-test-wasm-js",
    )
}

/**
 * A "custom" (alternative) Kotlin/JS or Kotlin/Wasm compiler and the relevant artifacts (stdlib, kotlin-test)
 * which are used in KLIB backward/forward compatibility tests.
 */
interface CustomWebCompilerSettings {
    val version: String
    val stdlib: File
    val kotlinTest: File
    val customCompiler: CustomCompiler
}

val CustomWebCompilerSettings.defaultLanguageVersion: LanguageVersion
    get() = LanguageVersion.fromFullVersionString(version)
        ?: fail("Cannot deduce the default LV from the compiler version: $version")

private fun createCustomWebCompilerSettings(
    compilerClasspathPropertyName: String,
    runtimeDependenciesPropertyName: String,
    versionPropertyName: String,
    stdlibArtifactName: String,
    kotlinTestArtifactName: String,
): CustomWebCompilerSettings = object : CustomWebCompilerSettings {
    private val artifacts: CustomCompilerArtifacts by lazy {
        CustomCompilerArtifacts.create(compilerClasspathPropertyName, runtimeDependenciesPropertyName, versionPropertyName)
    }

    override val version: String get() = artifacts.version
    override val stdlib: File by lazy { artifacts.runtimeDependency(stdlibArtifactName, "klib") }

    override val kotlinTest: File by lazy {
        // Older versions of Kotlin/JS 'kotlin-test' had KLIBs with *.jar file extension.
        artifacts.runtimeDependency(kotlinTestArtifactName, "klib", "jar")
    }

    override val customCompiler: CustomCompiler by lazy {
        CustomCompiler(artifacts.compilerClassPath, "org.jetbrains.kotlin.cli.js.K2JSCompiler", "execFullPathsInMessages")
    }
}
