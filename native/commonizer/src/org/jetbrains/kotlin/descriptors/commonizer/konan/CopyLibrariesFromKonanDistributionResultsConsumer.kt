/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.KonanDistribution
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer
import org.jetbrains.kotlin.descriptors.commonizer.klibDir
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.util.Logger
import java.io.File

internal fun CopyStdlibResultsConsumer(
    konanDistribution: KonanDistribution,
    destination: File,
    logger: Logger
): ResultsConsumer {
    return CopyLibrariesFromKonanDistributionResultsConsumer(
        konanDistribution,
        destination,
        invokeWhenCopied = { logger.log("Copied standard library") },
        copyFileIf = { file -> file.endsWith(KONAN_STDLIB_NAME) }
    )
}

internal fun CopyEndorsedLibrairesResultsConsumer(
    konanDistribution: KonanDistribution,
    destination: File,
    logger: Logger
): ResultsConsumer {
    return CopyLibrariesFromKonanDistributionResultsConsumer(
        konanDistribution,
        destination,
        invokeWhenCopied = { logger.log("Copied endorsed libraries") },
        copyFileIf = { file -> !file.endsWith(KONAN_STDLIB_NAME) }
    )
}

private class CopyLibrariesFromKonanDistributionResultsConsumer(
    private val konanDistribution: KonanDistribution,
    private val destination: File,
    private val invokeWhenCopied: () -> Unit = {},
    private val copyFileIf: (File) -> Boolean = { true }
) : ResultsConsumer {
    override fun allConsumed(status: ResultsConsumer.Status) {
        konanDistribution.klibDir
            .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            .listFiles().orEmpty()
            .filter { it.isDirectory }
            .filterNot(copyFileIf)
            .forEach { libraryOrigin ->
                val libraryDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryOrigin.name)
                libraryOrigin.copyRecursively(libraryDestination)
            }
        invokeWhenCopied()
    }
}
