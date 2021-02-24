/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.library.*
import java.io.File

internal data class KonanDistribution(val root: File)

internal val KonanDistribution.stdlib: File
    get() = root.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))

internal val KonanDistribution.klibDir: File
    get() = root.resolve(KONAN_DISTRIBUTION_KLIB_DIR)

internal val KonanDistribution.platformLibsDir: File
    get() = klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
