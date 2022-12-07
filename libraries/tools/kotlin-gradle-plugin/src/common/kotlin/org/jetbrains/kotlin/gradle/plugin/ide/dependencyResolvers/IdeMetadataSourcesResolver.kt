/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.gradle.utils.named

internal fun IdeMetadataSourcesResolver(): IdePlatformDependencyResolver = IdePlatformDependencyResolver(
    binaryType = IdeaKotlinDependency.SOURCES_BINARY_TYPE,
    artifactResolutionStrategy = IdePlatformDependencyResolver.ArtifactResolutionStrategy.PlatformLikeSourceSet(
        setupPlatformResolutionAttributes = {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
            attribute(Usage.USAGE_ATTRIBUTE, it.project.objects.named(Usage.JAVA_RUNTIME))
        },
        setupArtifactViewAttributes = {
            attribute(Category.CATEGORY_ATTRIBUTE, it.project.objects.named(Category.DOCUMENTATION))
            attribute(DocsType.DOCS_TYPE_ATTRIBUTE, it.project.objects.named(DocsType.SOURCES))
        }
    )
)
