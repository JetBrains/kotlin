/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.KdBundle

/**
 * Analyzes the given Kotlin (Multiplatform) [sourceSets] and produces a [KdBundle] describing their public API
 * surface.
 *
 * The underlying analysis is an implementation detail: callers only see [sourceSets] as input and [KdBundle] as
 * output.
 */
public fun generateDocumentationBundle(moduleName: String, sourceSets: List<KdAnalyzerSourceSet>): KdBundle {
    require(sourceSets.isNotEmpty()) { "At least one source set is required." }
    val names = sourceSets.map { it.name }
    require(names.size == names.toSet().size) { "Source set names must be unique, got: $names" }
    for (sourceSet in sourceSets) {
        require(sourceSet.dependsOn.all { it in names }) {
            "Source set '${sourceSet.name}' depends on unknown source set(s): ${sourceSet.dependsOn - names.toSet()}"
        }
    }

    // TODO(KT-90194): analyze the source sets and build a real bundle.
    return KdBundle(schemaVersion = KotlinVersion.CURRENT.toString(), fragments = emptyList(), expectActualMappings = emptyList())
}
