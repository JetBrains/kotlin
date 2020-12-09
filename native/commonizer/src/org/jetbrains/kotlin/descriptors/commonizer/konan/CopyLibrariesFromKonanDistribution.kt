/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.KonanDistribution
import org.jetbrains.kotlin.descriptors.commonizer.Result
import org.jetbrains.kotlin.descriptors.commonizer.klibDir
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.util.Logger
import java.io.File


internal fun CopyStdlibCommonizerResultSerializer(
    konanDistribution: KonanDistribution,
    destination: File,
    logger: Logger
): CommonizerResultSerializer {
    return CopyLibraryFromKonanDistributionSerializer(
        konanDistribution,
        destination,
        logger,
        copyFileIf = { file -> file.endsWith(KONAN_STDLIB_NAME) }
    )
}

internal fun CopyEndorsedCommonizerResultSerializer(
    konanDistribution: KonanDistribution,
    destination: File,
    logger: Logger
): CommonizerResultSerializer {
    return CopyLibraryFromKonanDistributionSerializer(
        konanDistribution,
        destination,
        logger,
        copyFileIf = { file -> !file.endsWith(KONAN_STDLIB_NAME) }
    )
}


private class CopyLibraryFromKonanDistributionSerializer(
    private val konanDistribution: KonanDistribution,
    private val destination: File,
    private val logger: Logger,
    private val copyFileIf: (File) -> Boolean = { true }
) : CommonizerResultSerializer {
    override fun invoke(originalLibraries: AllNativeLibraries, commonizerResult: Result) {
        konanDistribution.klibDir
            .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            .listFiles().orEmpty()
            .filter { it.isDirectory }
            .filterNot(copyFileIf)
            .forEach { libraryOrigin ->
                val libraryDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(libraryOrigin.name)
                libraryOrigin.copyRecursively(libraryDestination)
            }
        logger.log("Copied endorsed libraries")
    }
}
