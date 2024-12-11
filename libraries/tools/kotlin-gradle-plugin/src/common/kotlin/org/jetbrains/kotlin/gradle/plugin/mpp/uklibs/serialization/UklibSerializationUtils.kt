/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * // FIXME: Test this!!!
 *
 * All of the rezipping happens at execution time in ArchiveUklibTask:
 * - if the incoming fragment is a .klib or a .jar, just unzip it and zip into the .uklib zip
 * - if the incoming fragment is a directory, probably it's an unpacked klib, so just copy it
 */
private val allowRepackingArchivesWithExtensions = setOf(
    "klib",
    "jar",
)

internal fun zipUklibContents(
    manifest: String,
    fragmentToArtifact: Map<String, File>,
    outputZip: File,
    temporariesDirectory: File,
) {
    ZipOutputStream(
        BufferedOutputStream(
            FileOutputStream(outputZip)
        )
    ).use { zipOutputStream ->
        // Pack the manifest
        zipOutputStream.putNextEntry(ZipEntry(Uklib.UMANIFEST_FILE_NAME))
        manifest.byteInputStream().copyTo(zipOutputStream)

        fragmentToArtifact.forEach { (identifier, file) ->
            // Assume we are handling unpacked metadata and platform klibs
            if (file.isDirectory) {
                packDirectory(file, identifier, zipOutputStream)
            } else if (file.extension in allowRepackingArchivesWithExtensions) {
                val temp = temporariesDirectory.resolve(identifier)
                if (temp.exists()) temp.deleteDirectoryContents()
                temp.mkdirs()
                unzip(
                    zipFilePath = file,
                    outputFolderPath = temp,
                )
                packDirectory(
                    directory = temp,
                    identifier = identifier,
                    zipOutputStream = zipOutputStream,
                )
            } else {
                error("Trying to pack invalid file in uklib: ${file}")
            }
        }
        zipOutputStream.closeEntry()
    }
}

private fun packDirectory(
    directory: File,
    identifier: String,
    zipOutputStream: ZipOutputStream
) {
    Files.walk(directory.toPath()).forEach { path ->
        val zipEntry = ZipEntry(identifier + "/" + path.toFile().toRelativeString(directory))
        if (!Files.isDirectory(path)) {
            zipOutputStream.putNextEntry(zipEntry)
            Files.newInputStream(path).use { inputStream ->
                inputStream.copyTo(zipOutputStream)
            }
            zipOutputStream.closeEntry()
        }
    }
}

private fun unzip(zipFilePath: File, outputFolderPath: File) {
    ZipInputStream(FileInputStream(zipFilePath)).use { zipInputStream ->
        var zipEntry: ZipEntry? = zipInputStream.nextEntry
        while (zipEntry != null) {
            val newFile = File(outputFolderPath, zipEntry.name)
            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                FileOutputStream(newFile).use { fileOutputStream ->
                    zipInputStream.copyTo(fileOutputStream)
                }
            }
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.closeEntry()
    }
}