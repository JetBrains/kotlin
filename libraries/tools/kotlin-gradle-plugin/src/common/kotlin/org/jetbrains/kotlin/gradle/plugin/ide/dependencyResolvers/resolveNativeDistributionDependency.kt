/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.compilerRunner.konanVersion
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.library.*
import java.io.File

internal fun Project.resolveNativeDistributionLibraryForIde(
    library: File,
    target: CommonizerTarget,
    logger: Logger? = null
): IdeaKotlinResolvedBinaryDependency? {
    val resolvedLibrary = try {
        resolveSingleFileKlib(
            libraryFile = org.jetbrains.kotlin.konan.file.File(library.absolutePath),
            strategy = ToolingSingleFileKlibResolveStrategy
        )
    } catch (error: Throwable) {
        logger?.error("Failed to resolve library ${library.path}", error)
        return null
    }

    return IdeaKotlinResolvedBinaryDependency(
        binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
        classpath = IdeaKotlinClasspath(library),
        coordinates = IdeaKotlinBinaryCoordinates(
            group = "org.jetbrains.kotlin.native",
            module = resolvedLibrary.shortName ?: resolvedLibrary.uniqueName.split(".").last(),
            version = project.konanVersion,
            sourceSetName = target.identityString
        ),
    ).apply {
        isNativeDistribution = true
        klibExtra = KlibExtra(resolvedLibrary)
    }
}


