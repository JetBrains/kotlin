/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import java.io.File

object DependencyDirectories {

    private const val DEPENDENCIES_FOLDER_NAME = "dependencies"
    private const val CACHE_FOLDER_NAME = "cache"

    @JvmStatic
    val localKonanDir: File
        get() = getLocalKonanDir()

    @JvmStatic
    val defaultDependenciesRoot: File
        get() = getDependenciesRoot()

    fun getLocalKonanDir(konanDataDir: String? = null): File {
        return File(
            konanDataDir
                ?: System.getenv("KONAN_DATA_DIR")
                ?: (System.getProperty("user.home") + File.separator + ".konan")
        )
    }

    fun getDependenciesRoot(konanDataDir: String? = null): File {
        return getLocalKonanDir(konanDataDir).resolve(DEPENDENCIES_FOLDER_NAME)
    }

    fun getDependencyCacheDir(konanDataDir: String? = null): File {
        return getLocalKonanDir(konanDataDir).resolve(CACHE_FOLDER_NAME)
    }
}
