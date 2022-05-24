/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

internal object IdeaKpmNativeStdlibDependencyResolver : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        return setOf(
            IdeaKpmResolvedBinaryDependencyImpl(
                binaryType = IdeaKpmDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = KonanDistribution(fragment.project.konanHome).stdlib,
                coordinates = IdeaKpmBinaryCoordinatesImpl(
                    "org.jetbrains.kotlin", "stdlib-native", fragment.project.getKotlinPluginVersion()
                )
            )
        )
    }
}
