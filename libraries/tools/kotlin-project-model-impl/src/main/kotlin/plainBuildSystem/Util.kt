/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.plainBuildSystem

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.outputStream
import kotlin.streams.asSequence

internal fun zip(src: Path, dst: Path) {
    Files.deleteIfExists(dst)
    dst.parent.createDirectories()

    ZipOutputStream(dst.outputStream()).use { zos ->
        Files.walk(src)
            .asSequence()
            .forEach { entry ->
                if (entry.isRegularFile()) {
                    zos.putNextEntry(ZipEntry(src.relativize(entry).toString()))
                    Files.copy(entry, zos)
                } else {
                    zos.putNextEntry(ZipEntry(src.relativize(entry).toString() + "/"))
                }
                zos.closeEntry()
            }
    }
}