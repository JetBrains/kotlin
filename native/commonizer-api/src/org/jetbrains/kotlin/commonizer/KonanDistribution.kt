/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.konan.library.*
import java.io.File

public data class KonanDistribution(val root: File) {
    public constructor(rootPath: String) : this(File(rootPath))
}

public val KonanDistribution.konanCommonLibraries: File
    get() = root.resolve(KONAN_DISTRIBUTION_KLIB_DIR).resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)

public val KonanDistribution.stdlib: File
    get() = konanCommonLibraries.resolve(KONAN_STDLIB_NAME)

public val KonanDistribution.klibDir: File
    get() = root.resolve(KONAN_DISTRIBUTION_KLIB_DIR)

public val KonanDistribution.platformLibsDir: File
    get() = klibDir.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)

public val KonanDistribution.sourcesDir: File
    get() = root.resolve(KONAN_DISTRIBUTION_SOURCES_DIR)

public val KonanDistribution.toolsDir: File
    get() = root.resolve(KONAN_DISTRIBUTION_TOOLS_DIR)

/**
 * Directory containing compiled klibs for the commonizer support library.
 * Mirrors the `build/classes/kotlin/` layout of the `commonizer-support-library` project:
 *   - `<target>/main/klib/commonizer-support-library/` for leaf targets
 *   - `metadata/<sourceSet>/klib/commonizer-support-library_<sourceSet>/` for intermediate source sets
 */
public val KonanDistribution.commonizerSupportLibrary: File
    get() = root.resolve("klib/commonizer/support")
