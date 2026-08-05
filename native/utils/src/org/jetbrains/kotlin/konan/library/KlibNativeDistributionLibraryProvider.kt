/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.io.listDirectoryEntriesIfDirectoryExists
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.loader.KlibLibraryProvider
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * A component that helps to load libraries from the Kotlin/Native distribution.
 */
class KlibNativeDistributionLibraryProvider(
    nativeHome: Path,
    init: KlibNativeDistributionLibraryProviderSpec.() -> Unit,
) : KlibLibraryProvider {
    constructor(
        nativeHome: File,
        init: KlibNativeDistributionLibraryProviderSpec.() -> Unit,
    ) : this(nativeHome.toPath(), init)

    // Only attempt to resolve the path to a canonical path if the directory exists.
    // Otherwise, we might end up with a Java IO exception.
    private val canonicalNativeHome: Path = if (nativeHome.exists()) nativeHome.toRealPath() else nativeHome

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
            this += canonicalNativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
                .resolve(KONAN_STDLIB_NAME)
                .pathString
        }

        withPlatformLibsForTarget?.let { target ->
            canonicalNativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
                .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
                .resolve(target.visibleName)
                .listDirectoryEntriesIfDirectoryExists()
                .mapNotNullTo(this) { if (it.isDirectory() && it.name.startsWith(KONAN_PLATFORM_LIBS_NAME_PREFIX)) it.pathString else null }
        }
    }

    override fun postProcessLoadedLibrary(klib: Klib, wasLoadedByTheCurrentProvider: Boolean) {
        klib.isFromKotlinNativeDistribution = klib.canonicalPath.startsWith(canonicalNativeHome)
        klib.isImplicitlyLoadedFromKotlinNativeDistribution = wasLoadedByTheCurrentProvider
    }
}

interface KlibNativeDistributionLibraryProviderSpec {
    fun withStdlib()
    fun withPlatformLibs(target: KonanTarget)
}
