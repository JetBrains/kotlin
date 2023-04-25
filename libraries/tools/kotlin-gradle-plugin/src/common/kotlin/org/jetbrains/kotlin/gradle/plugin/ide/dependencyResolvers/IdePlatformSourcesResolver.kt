/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.utils.named

internal fun IdePlatformSourcesResolver(): IdeBinaryDependencyResolver = IdeBinaryDependencyResolver(
    binaryType = IdeDependencyResolver.SOURCES_BINARY_TYPE,
    artifactResolutionStrategy = IdeBinaryDependencyResolver.ArtifactResolutionStrategy.Compilation(
        setupArtifactViewAttributes = {
            attribute(Category.CATEGORY_ATTRIBUTE, it.project.objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, it.project.objects.named(DocsType.SOURCES))
        }
    )
)
