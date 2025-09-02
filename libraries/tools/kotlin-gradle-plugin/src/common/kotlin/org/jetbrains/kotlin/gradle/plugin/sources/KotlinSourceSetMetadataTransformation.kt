/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.GranularMetadataTransformation
import org.jetbrains.kotlin.gradle.plugin.mpp.ParentSourceSetVisibilityProvider
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.utils.extrasStoredProperty

/**
 * Returns [GranularMetadataTransformation] for all requested compile dependencies
 * scopes: API, IMPLEMENTATION, COMPILE_ONLY; See [KotlinDependencyScope.compileScopes]
 */
internal val InternalKotlinSourceSet.metadataTransformation: GranularMetadataTransformation? by extrasStoredProperty property@{
    // Create only for source sets in multiplatform plugin
    project.multiplatformExtensionOrNull ?: return@property null

    val parentSourceSetVisibilityProvider = ParentSourceSetVisibilityProvider { componentIdentifier ->
        dependsOnClosureWithInterCompilationDependencies(this).filterIsInstance<DefaultKotlinSourceSet>()
            .mapNotNull { it.metadataTransformation }
            .flatMap { it.visibleSourceSetsByComponentId[componentIdentifier].orEmpty() }
            .toSet()
    }

    val granularMetadataTransformation = GranularMetadataTransformation(
        params = GranularMetadataTransformation.Params(project, this, transformProjectDependencies = false),
        parentSourceSetVisibilityProvider = parentSourceSetVisibilityProvider,
    )

    granularMetadataTransformation
}