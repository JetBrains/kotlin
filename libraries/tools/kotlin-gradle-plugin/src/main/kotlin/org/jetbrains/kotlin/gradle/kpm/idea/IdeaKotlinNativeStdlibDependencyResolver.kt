/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

internal object IdeaKotlinNativeStdlibDependencyResolver : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> {
        return setOf(
            IdeaKotlinResolvedBinaryDependencyImpl(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = KonanDistribution(fragment.project.konanHome).stdlib,
                coordinates = IdeaKotlinBinaryCoordinatesImpl(
                    "org.jetbrains.kotlin", "stdlib-native", fragment.project.getKotlinPluginVersion()
                )
            )
        )
    }
}
