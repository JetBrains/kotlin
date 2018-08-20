/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.util

import java.io.File

object DependencyDirectories {
    val localKonanDir: File by lazy {
        File(System.getenv("KONAN_DATA_DIR") ?: (System.getProperty("user.home") + File.separator + ".konan"))
    }

    @JvmStatic
    val defaultDependenciesRoot: File
        get() = localKonanDir.resolve("dependencies")

    val defaultDependencyCacheDir: File
        get() = localKonanDir.resolve("cache")
}
