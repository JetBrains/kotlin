/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

internal object IdeaKpmUnusedSourcesAndDocumentationFilter : IdeaKpmDependencyTransformer {
    override fun transform(
        fragment: GradleKpmFragment, dependencies: Set<IdeaKpmDependency>
    ): Set<IdeaKpmDependency> {
        val sourcesAndDocumentationDependencies = dependencies
            .filterIsInstance<IdeaKpmResolvedBinaryDependency>()
            .filter { dependency -> dependency.isSourcesType || dependency.isDocumentationType }
            .toSet()

        val classpathCoordinates = dependencies.filterIsInstance<IdeaKpmResolvedBinaryDependency>()
            .filter { dependency -> dependency.isClasspathType }
            .mapNotNull { it.coordinates }
            .toSet()

        val unusedSourcesAndDocumentationDependencies = sourcesAndDocumentationDependencies
            .filter { it.coordinates !in classpathCoordinates }
            .toSet()

        return dependencies - unusedSourcesAndDocumentationDependencies
    }
}
