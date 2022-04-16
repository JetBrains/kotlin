/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.containingVariants

internal object IdeaKotlinSinglePlatformStdlibCommonFilter : IdeaKotlinDependencyTransformer {
    private const val stdlibCoordinatesGroup = "org.jetbrains.kotlin"
    private val stdlibCoordinatesModules = setOf("kotlin-stdlib-common", "kotlin-test-common", "kotlin-test-annotations-common")

    override fun transform(
        fragment: KotlinGradleFragment,
        dependencies: Set<IdeaKotlinDependency>
    ): Set<IdeaKotlinDependency> {
        val platforms = fragment.containingVariants.map { it.platformType }.toSet()
        if (platforms.size != 1) return dependencies

        return dependencies.filter { dependency ->
            val binaryDependency = dependency as? IdeaKotlinBinaryDependency ?: return@filter true
            val coordinates = binaryDependency.coordinates ?: return@filter true
            coordinates.group != stdlibCoordinatesGroup || coordinates.module !in stdlibCoordinatesModules
        }.toSet()
    }
}
