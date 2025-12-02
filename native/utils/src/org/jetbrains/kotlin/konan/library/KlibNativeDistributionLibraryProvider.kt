/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.loader.KlibLibraryProvider
import java.io.File

/**
 * A component that helps to load libraries from the Kotlin/Native distribution.
 */
class KlibNativeDistributionLibraryProvider(
    private val nativeHome: File,
    init: KlibNativeDistributionLibraryProviderSpec.() -> Unit
) : KlibLibraryProvider {
    private var withStdlib = false
    private var withPlatformLibsForTarget: KonanTarget? = null

    init {
        object : KlibNativeDistributionLibraryProviderSpec {
            override fun withStdlib() {
                withStdlib = true
            }

            override fun withPlatformLibs(target: KonanTarget) {
                withPlatformLibsForTarget = target
            }
        }.init()
    }

    override fun getLibraryPaths(): List<String> = buildList {
        if (withStdlib) {
            this += nativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                .resolve(KONAN_STDLIB_NAME)
                .path
        }

        withPlatformLibsForTarget?.let { target ->
            nativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
                .resolve(target.visibleName)
                .listFiles()
                ?.mapNotNullTo(this) { if (it.isDirectory) it.path else null }
        }
    }

    override fun postProcessLoadedLibrary(klib: Klib) {
        klib.isFromKotlinNativeDistribution = true
    }
}

interface KlibNativeDistributionLibraryProviderSpec {
    fun withStdlib()
    fun withPlatformLibs(target: KonanTarget)
}
