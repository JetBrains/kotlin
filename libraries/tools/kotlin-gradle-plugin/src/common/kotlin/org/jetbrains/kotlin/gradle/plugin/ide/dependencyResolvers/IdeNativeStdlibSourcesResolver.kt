/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.sourcesDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeStdlib
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.sources.project

internal object IdeNativeStdlibSourcesResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return KonanDistribution(sourceSet.project.konanHome).sourcesDir.listFiles().orEmpty()
            /* Ignore org.jetbrains.kotlinx. in this case */
            .filter { file -> file.name.startsWith("kotlin") }
            .map { file ->
                IdeaKotlinResolvedBinaryDependency(
                    binaryType = IdeaKotlinDependency.SOURCES_BINARY_TYPE,
                    binaryFile = file,
                    coordinates = IdeNativeStdlibDependencyResolver.nativeStdlibCoordinates(sourceSet.project)
                ).apply {
                    isNativeStdlib = true
                    isNativeDistribution = true
                }
            }.toSet()
    }
}
