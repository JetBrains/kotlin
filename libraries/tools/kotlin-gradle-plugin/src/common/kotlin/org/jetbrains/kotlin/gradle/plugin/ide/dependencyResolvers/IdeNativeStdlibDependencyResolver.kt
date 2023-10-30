/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.sourcesDir
import org.jetbrains.kotlin.commonizer.stdlib
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeStdlib
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.idea.tcs.extras.sourcesClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.resolveSingleFileKlib

internal object IdeNativeStdlibDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val konanDistribution = KonanDistribution(sourceSet.project.konanHome)
        val stdlibFile = KonanDistribution(sourceSet.project.konanHome).stdlib

        val klibExtra = try {
            val kotlinLibrary = resolveSingleFileKlib(
                libraryFile = File(stdlibFile.absolutePath),
                strategy = ToolingSingleFileKlibResolveStrategy
            )

            KlibExtra(kotlinLibrary)
        } catch (error: Throwable) {
            null
        }

        return setOf(
            IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
                classpath = IdeaKotlinClasspath(stdlibFile),
                coordinates = nativeStdlibCoordinates(sourceSet.project)
            ).apply {
                this.isNativeDistribution = true
                this.isNativeStdlib = true
                this.klibExtra = klibExtra
                this.sourcesClasspath += konanDistribution.sourcesDir.listFiles().orEmpty()
                    /* Ignore org.jetbrains.kotlinx. in this case */
                    .filter { file -> file.name.startsWith("kotlin") }
                    .ifEmpty { return emptySet() }
            }
        )
    }

    fun nativeStdlibCoordinates(project: Project): IdeaKotlinBinaryCoordinates = IdeaKotlinBinaryCoordinates(
        "org.jetbrains.kotlin.native", "stdlib", project.konanVersion,
    )
}
