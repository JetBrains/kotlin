/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.Platform
import org.jetbrains.kotlin.documentation.analyzer.KdAnalyzerPlatform
import org.jetbrains.kotlin.documentation.analyzer.KdAnalyzerSourceSet

/**
 * Builds the [DokkaConfigurationImpl] used to drive the (internal) Dokka analysis pipeline out of the public,
 * KDM-shaped [KdAnalyzerSourceSet] inputs.
 *
 * Every [org.jetbrains.dokka.DokkaConfiguration.Visibility] is requested (not just `public`), since filtering
 * by visibility is a concern of KDM consumers, not of the model itself: every declaration ends up in the model
 * with its own [org.jetbrains.kotlin.documentation.KdVisibility].
 */
internal fun buildDokkaConfiguration(moduleName: String, sourceSets: List<KdAnalyzerSourceSet>): DokkaConfigurationImpl =
    DokkaConfigurationImpl(
        moduleName = moduleName,
        sourceSets = sourceSets.map { it.toDokkaSourceSet(moduleName) },
        finalizeCoroutines = false,
    )

private fun KdAnalyzerSourceSet.toDokkaSourceSet(scopeId: String): DokkaSourceSetImpl = DokkaSourceSetImpl(
    displayName = name,
    sourceSetID = DokkaSourceSetID(scopeId, name),
    classpath = classpath,
    sourceRoots = sourceRoots,
    dependentSourceSets = dependsOn.map { DokkaSourceSetID(scopeId, it) }.toSet(),
    analysisPlatform = platform.toDokkaPlatform(),
    documentedVisibilities = DokkaConfiguration.Visibility.entries.toSet(), // TODO KT-90416
    skipEmptyPackages = false,
)

private fun KdAnalyzerPlatform.toDokkaPlatform(): Platform = when (this) {
    KdAnalyzerPlatform.JVM -> Platform.jvm
    KdAnalyzerPlatform.JS -> Platform.js
    KdAnalyzerPlatform.WASM_JS -> Platform.wasmJs
    KdAnalyzerPlatform.WASM_WASI -> Platform.wasmWasi
    KdAnalyzerPlatform.NATIVE -> Platform.native
    KdAnalyzerPlatform.COMMON -> Platform.common
}
