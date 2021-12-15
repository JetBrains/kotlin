/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import kotlin.time.Duration

/**
 * The tested and the host Kotlin/Native targets.
 */
internal class KotlinNativeTargets(val testTarget: KonanTarget, val hostTarget: KonanTarget)

/**
 * The Kotlin/Native home.
 */
internal class KotlinNativeHome(val dir: File) {
    val path: String get() = dir.path
}

/**
 * Lazy-initialized class loader with the Kotlin/Native embedded compiler.
 */
internal class KotlinNativeClassLoader(private val lazyClassLoader: Lazy<ClassLoader>) {
    val classLoader: ClassLoader get() = lazyClassLoader.value
}

// TODO: in fact, only WITH_MODULES mode is supported now
internal enum class TestMode(val description: String) {
    ONE_STAGE(
        description = "Compile test files altogether without producing intermediate KLIBs."
    ),
    TWO_STAGE(
        description = "Compile test files altogether and produce an intermediate KLIB. Then produce a program from the KLIB using -Xinclude."
    ),
    WITH_MODULES(
        description = "Compile each test file as one or many modules (depending on MODULE directives declared in the file)." +
                " Then link the KLIBs into the single executable file."
    )
}

/**
 * Current project's directories.
 */
internal class BaseDirs(val buildDir: File)

/**
 * Timeouts.
 */
internal class Timeouts(val executionTimeout: Duration)

/**
 * Used cache kind.
 */
internal sealed interface CacheKind {
    object WithoutCache : CacheKind

    object WithStaticCache : CacheKind {
        fun getRootCacheDirectory(
            kotlinNativeHome: KotlinNativeHome,
            kotlinNativeTargets: KotlinNativeTargets,
            debuggable: Boolean
        ): File? = kotlinNativeHome.dir
            .resolve("klib/cache")
            .resolve(computeCacheDirName(kotlinNativeTargets.testTarget, CACHE_KIND, debuggable))
            .takeIf { it.exists() }

        private const val CACHE_KIND = "STATIC"
    }

    companion object {
        private fun computeCacheDirName(testTarget: KonanTarget, cacheKind: String, debuggable: Boolean) =
            "$testTarget${if (debuggable) "-g" else ""}$cacheKind"
    }
}

internal fun Settings.getRootCacheDirectory(debuggable: Boolean): File? =
    get<CacheKind>().safeAs<CacheKind.WithStaticCache>()?.getRootCacheDirectory(get(), get(), debuggable)
