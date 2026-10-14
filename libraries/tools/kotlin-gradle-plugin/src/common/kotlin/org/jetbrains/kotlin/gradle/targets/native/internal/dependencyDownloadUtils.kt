/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.konan.util.ArchiveType
import org.jetbrains.kotlin.konan.util.DependencyExtractor
import java.io.File

internal fun unzipTo(archive: File, toDirectory: File) {
    when {
        archive.name.endsWith("zip") -> DependencyExtractor().extract(archive, toDirectory, ArchiveType.ZIP)
        archive.name.endsWith(".tar.gz") -> DependencyExtractor().extract(archive, toDirectory, ArchiveType.TAR_GZ)
        else -> error("Unsupported format for unzipping $archive")
    }
}
