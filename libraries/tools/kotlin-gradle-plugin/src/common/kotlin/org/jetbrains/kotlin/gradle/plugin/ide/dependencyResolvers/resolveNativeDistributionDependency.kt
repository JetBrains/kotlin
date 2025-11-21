/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.identityString
import org.jetbrains.kotlin.commonizer.konanTargets
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.tcs.extras.klibExtra
import org.jetbrains.kotlin.gradle.plugin.ide.KlibExtra
import org.jetbrains.kotlin.gradle.targets.native.internal.fakeCommonizedNativeDistributionKlibs
import org.jetbrains.kotlin.gradle.utils.loadSingleKlib
import org.jetbrains.kotlin.library.*
import java.io.File

internal fun resolveNativeDistributionLibraryForIde(
    library: File,
    target: CommonizerTarget,
    kotlinNativeVersion: String,
    logger: Logger
): IdeaKotlinResolvedBinaryDependency? {
    val resolvedLibrary = loadSingleKlib(library, logger) ?: return null

    val isFakeNativeDistributionDependency = library.name == fakeCommonizedNativeDistributionKlibs
    val module = if (!isFakeNativeDistributionDependency) {
        resolvedLibrary.shortName ?: resolvedLibrary.uniqueName.split(".").last()
    } else {
        fakeCommonizedNativeDistributionKlibs
    }

    return IdeaKotlinResolvedBinaryDependency(
        binaryType = IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE,
        classpath = IdeaKotlinClasspath(library),
        coordinates = IdeaKotlinBinaryCoordinates(
            group = "org.jetbrains.kotlin.native",
            module = module,
            version = kotlinNativeVersion,
            sourceSetName = target.identityString
        ),
    ).apply {
        isNativeDistribution = true
        klibExtra = if (isFakeNativeDistributionDependency) {
            // fake data for tests
            org.jetbrains.kotlin.gradle.idea.tcs.extras.KlibExtra(
                builtInsPlatform = "Native",
                uniqueName = "org.jetbrains.kotlin.native.stdlib",
                shortName = "stdlib",
                packageFqName = "kotlin",
                nativeTargets = target.konanTargets.map { it.name },
                commonizerNativeTargets = target.konanTargets.map { it.name },
                commonizerTarget = target.identityString,
                isInterop = true
            )
        } else {
            KlibExtra(resolvedLibrary)
        }
    }
}


