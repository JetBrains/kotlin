/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.File
import java.io.IOException
import java.nio.file.Files

internal fun File.expandSymlinks(): List<String> {
    if (!Files.isSymbolicLink(toPath())) return listOf(path)
    val realPath = try {
        toPath().toRealPath().toString()
    } catch (_: IOException) {
        return listOf(path) // dangling symlink
    }
    return listOf(path, realPath)
}
