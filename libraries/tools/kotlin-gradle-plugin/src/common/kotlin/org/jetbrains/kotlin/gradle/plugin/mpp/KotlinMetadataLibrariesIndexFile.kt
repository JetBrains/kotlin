/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import java.io.File

/**
 * Files used by the [MetadataDependencyTransformationTask] and [CInteropMetadataDependencyTransformationTask] to
 * store the resulting 'metadata path' in this index file.
 */
internal class KotlinMetadataLibrariesIndexFile(private val file: File) {
    fun read(): List<File> = file.readLines().map { File(it) }

    fun write(files: Iterable<File>) {
        val content = files.joinToString(System.lineSeparator()) { it.path }
        file.writeText(content)
    }
}
