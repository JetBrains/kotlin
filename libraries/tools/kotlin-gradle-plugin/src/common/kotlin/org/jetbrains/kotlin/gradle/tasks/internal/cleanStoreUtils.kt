/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

internal fun File.cleanDir(expirationDate: Instant) {
    fun modificationDate(file: Path): Instant {
        return Files.getLastModifiedTime(file).toInstant()
    }

    listFiles()
        ?.filter { file ->
            modificationDate(file.toPath()).isBefore(expirationDate)
        }
        ?.forEach { file -> file.deleteRecursively() }
}
