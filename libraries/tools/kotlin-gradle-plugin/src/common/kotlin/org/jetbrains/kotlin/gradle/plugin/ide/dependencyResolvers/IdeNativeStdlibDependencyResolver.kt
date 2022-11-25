/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal object IdeNativeStdlibDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return setOf(
            IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = KonanDistribution(sourceSet.project.konanHome).stdlib,
                extras = mutableExtrasOf(),
                coordinates = IdeaKotlinBinaryCoordinates(
                    "org.jetbrains.kotlin", "stdlib-native", sourceSet.project.konanVersion.toString(),
                )
            )
        )
    }
}
