/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.io.InputStream
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile


/**
 * Use this [Path] as a [ZipFile].
 */
fun <T> Path.useAsZipFile(
    action: (zipFile: ZipFile) -> T,
): T = ZipFile(this.toFile()).use(action)


/**
 * Read the `default/manifest` of this `.klib` as a new [Properties] instance.
 *
 * (Assumes that this [ZipFile] is a valid Kotlin `.klib`)
 */
fun ZipFile.readKLibManifest(): Properties =
    useStreamEntry("default/manifest", InputStream::useToLoadProperties)


/**
 * Loads an [InputStream] from the [java.util.zip.ZipEntry] located at [path].
 *
 * @param path the path of the entry in the zip file.
 */
private fun <T> ZipFile.useStreamEntry(
    path: String,
    action: (stream: InputStream) -> T,
): T {
    val entry = getEntry(path)
    return getInputStream(entry).use(action)
}


/**
 * Loads this [InputStream] as a new [Properties] instance.
 *
 * This input stream is automatically closed after the properties are loaded.
 */
fun InputStream.useToLoadProperties(): Properties =
    use { reader ->
        Properties().apply { load(reader) }
    }
