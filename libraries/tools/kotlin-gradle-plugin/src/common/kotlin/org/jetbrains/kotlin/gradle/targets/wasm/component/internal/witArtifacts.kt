/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.component.internal

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal const val KOTLIN_WIT_ARTIFACT = "kotlin-wasm-wit"

/**
 * Name of the directory with WIT declarations inside a klib and inside a project.
 */
internal const val WIT_DIRECTORY_NAME = "wit"

/**
 * Directories with WIT declarations of all external runtime dependencies of [compilation].
 *
 * Every directory is the `wit` directory of a dependency klib extracted by [WitExtractionTransform],
 * dependencies without such a directory contribute nothing.
 *
 */
internal fun witDirectoriesFromRuntimeDependencies(compilation: KotlinJsIrCompilation): FileCollection {
    val project = compilation.project

    val runtimeDependencyConfiguration = compilation.configurations.runtimeDependencyConfiguration
        ?: error("Wasm compilation should contain runtime configuration")

    return project.filesProvider {
        runtimeDependencyConfiguration
            .incoming
            .artifactView { view ->
                view.attributes { attributes ->
                    attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, KOTLIN_WIT_ARTIFACT)
                }
            }
            .files
    }
}
