/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isIdeaProjectLevel
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib

internal object IdeNativeStdlibDependencyResolver : IdeDependencyResolver {
    private val logger: Logger = Logging.getLogger(IdeNativePlatformDependencyResolver::class.java)

    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val binaryFile = KonanDistribution(sourceSet.project.konanHome).stdlib

        val klibExtra = try {
            val kotlinLibrary = resolveSingleFileKlib(
                libraryFile = File(binaryFile.absolutePath),
                strategy = ToolingSingleFileKlibResolveStrategy
            )

            KlibExtra(kotlinLibrary)
        } catch (error: Throwable) {
            logger.error("Failed to resolve Native Stdlib", error)
            null
        }

        return setOf(
            IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = binaryFile,
                coordinates = IdeaKotlinBinaryCoordinates(
                    "org.jetbrains.kotlin.native", "stdlib", sourceSet.project.konanVersion.toString(),
                )
            ).apply {
                this.isIdeaProjectLevel = true
                this.klibExtra = klibExtra
            }
        )
    }
}
