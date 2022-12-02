/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.platformLibsDir
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.ide.IdeDependencyResolver
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.gradle.plugin.sources.project
import org.jetbrains.kotlin.gradle.targets.native.internal.getCommonizerTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import java.io.File

object IdeNativePlatformDependencyResolver : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        val commonizerTarget = getCommonizerTarget(sourceSet) as? LeafCommonizerTarget ?: return emptySet()
        val konanTarget = commonizerTarget.konanTargetOrNull ?: return emptySet()

        return sourceSet.project.konanDistribution.platformLibsDir.resolve(konanTarget.name)
            .listFiles().orEmpty()
            .filter { it.isDirectory || it.extension == KLIB_FILE_EXTENSION }
            .mapNotNull { libraryFile -> sourceSet.project.resolveKlib(libraryFile, konanTarget) }
            .toSet()
    }

    private val Project.konanDistribution: KonanDistribution
        get() = KonanDistribution(project.file(konanHome))

    private fun Project.resolveKlib(file: File, konanTarget: KonanTarget): IdeaKotlinResolvedBinaryDependency? {
        try {
            val kotlinLibrary = resolveSingleFileKlib(
                org.jetbrains.kotlin.konan.file.File(file.absolutePath),
                strategy = ToolingSingleFileKlibResolveStrategy
            )

            return IdeaKotlinResolvedBinaryDependency(
                binaryType = IdeaKotlinDependency.CLASSPATH_BINARY_TYPE,
                binaryFile = file,
                coordinates = IdeaKotlinBinaryCoordinates(
                    group = "org.jetbrains.kotlin.native",
                    module = kotlinLibrary.packageFqName ?: kotlinLibrary.shortName ?: kotlinLibrary.uniqueName,
                    version = project.konanVersion.toString(),
                    sourceSetName = konanTarget.toString()
                ),
            ).apply {
                isNativeDistribution = true
                klibExtra = KlibExtra(kotlinLibrary)
            }
        } catch (t: Throwable) {
            logger.error("Failed resolving library ${file.path}", t)
            return null
        }
    }
}
