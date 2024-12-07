/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyTransformers

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyTransformer

internal object IdePlatformStdlibCommonDependencyFilter : IdeDependencyTransformer {
    private const val stdlibCoordinatesGroup = "org.jetbrains.kotlin"
    private val stdlibCoordinatesModules = setOf("kotlin-stdlib-common", "kotlin-test-common", "kotlin-test-annotations-common")

    override fun transform(sourceSet: KotlinSourceSet, dependencies: Set<IdeaKotlinDependency>): Set<IdeaKotlinDependency> {
        return dependencies.filterNotTo(mutableSetOf()) { dependency ->
            dependency is IdeaKotlinResolvedBinaryDependency
                    && dependency.coordinates?.group == stdlibCoordinatesGroup
                    && dependency.coordinates?.module in stdlibCoordinatesModules
        }
    }
}
