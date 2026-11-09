/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import java.io.File

/** The target platform of a [KdAnalyzerSourceSet], mirroring Kotlin Multiplatform's supported platforms. */
public enum class KdAnalyzerPlatform {
    JVM, JS, WASM_JS, WASM_WASI, NATIVE, COMMON
}

/**
 * One Kotlin (Multiplatform) source set to analyze, e.g. `commonMain` or `jvmMain`.
 *
 * [name] becomes the corresponding `KdFragment.name` in the generated bundle, so it must be unique
 * within a single [generateDocumentationBundle] call.
 */
public class KdAnalyzerSourceSet(
    public val name: String,
    public val platform: KdAnalyzerPlatform,
    public val sourceRoots: Set<File>,
    public val classpath: List<File> = emptyList(),
    /** The names of the other [KdAnalyzerSourceSet]s (from the same call) that this source set depends on. */
    public val dependsOn: Set<String> = emptySet(),
)
