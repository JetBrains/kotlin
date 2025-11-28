/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

/**
 * A component that helps to load libraries from the Kotlin/Native distribution.
 */
class KlibNativeDistributionLibraryProvider(private val nativeHome: File) {
    private val libraries = ArrayList<File>()

    fun withStdlib(): KlibNativeDistributionLibraryProvider {
        libraries += nativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            .resolve(KONAN_STDLIB_NAME)
        return this
    }

    fun withPlatformLibs(target: KonanTarget): KlibNativeDistributionLibraryProvider {
        nativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(target.visibleName)
            .listFiles()
            ?.forEach { if (it.isDirectory) libraries += it }
        return this
    }

    fun getPaths(): List<String> = libraries.map { it.path }
}
