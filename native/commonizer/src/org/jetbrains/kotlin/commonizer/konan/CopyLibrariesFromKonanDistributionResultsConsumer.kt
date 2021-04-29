/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer.konan

import org.jetbrains.kotlin.commonizer.CommonizerParameters
import org.jetbrains.kotlin.commonizer.KonanDistribution
import org.jetbrains.kotlin.commonizer.ResultsConsumer
import org.jetbrains.kotlin.commonizer.klibDir
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import java.io.File

internal fun CopyStdlibResultsConsumer(
    konanDistribution: KonanDistribution,
    destination: File
): ResultsConsumer {
    return CopyLibrariesFromKonanDistributionResultsConsumer(
        konanDistribution,
        destination,
        invokeWhenCopied = { logger?.progress("Copied standard library") },
        copyFileIf = { file -> file.endsWith(KONAN_STDLIB_NAME) }
    )
}

internal fun CopyEndorsedLibrairesResultsConsumer(
    konanDistribution: KonanDistribution,
    destination: File,
): ResultsConsumer {
    return CopyLibrariesFromKonanDistributionResultsConsumer(
        konanDistribution,
        destination,
        invokeWhenCopied = { logger?.progress("Copied endorsed libraries") },
        copyFileIf = { file -> !file.endsWith(KONAN_STDLIB_NAME) }
    )
}

private class CopyLibrariesFromKonanDistributionResultsConsumer(
    private val konanDistribution: KonanDistribution,
    private val destination: File,
    private val invokeWhenCopied: CommonizerParameters.() -> Unit = {},
    private val copyFileIf: (File) -> Boolean = { true }
) : ResultsConsumer {
    override fun allConsumed(parameters: CommonizerParameters, status: ResultsConsumer.Status) {
        konanDistribution.klibDir
            .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            .listFiles().orEmpty()
            .filter { it.isDirectory }
            .filterNot(copyFileIf)
            .forEach { libraryOrigin ->
                val libraryDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryOrigin.name)
                libraryOrigin.copyRecursively(libraryDestination)
            }
        parameters.invokeWhenCopied()
    }
}
