/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.gradle.targets.native.internal.getCommonizerTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.getNativeDistributionDependencies
import org.jetbrains.kotlin.library.*
import java.io.File

internal class IdeaKpmNativePlatformDependencyResolver : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        val project = fragment.project
        val commonizerTarget = project.getCommonizerTarget(fragment) ?: return emptySet()

        return project.getNativeDistributionDependencies(commonizerTarget).files
            .filter { it.isDirectory || it.extension == "klib" }
            .mapNotNull { libraryFile -> project.resolveKlib(libraryFile) }
            .toSet()
    }
}

private fun Project.resolveKlib(file: File): IdeaKpmResolvedBinaryDependency? {
    try {
        val kotlinLibrary = resolveSingleFileKlib(
            org.jetbrains.kotlin.konan.file.File(file.absolutePath),
            strategy = ToolingSingleFileKlibResolveStrategy
        )

        val nativeTargets = kotlinLibrary.nativeTargets.sorted().joinToString(
            prefix = "(",
            postfix = ")"
        )

        return IdeaKpmResolvedBinaryDependencyImpl(
            binaryType = IdeaKpmDependency.CLASSPATH_BINARY_TYPE,
            binaryFile = file,
            coordinates = IdeaKpmBinaryCoordinatesImpl(
                group = "org.jetbrains.kotlin.native",
                module = (kotlinLibrary.shortName ?: kotlinLibrary.uniqueName) + "-" + nativeTargets,
                version = project.getKotlinPluginVersion()
            )
        )
    } catch (t: Throwable) {
        logger.error("Failed resolving library ${file.path}", t)
        return null
    }
}
