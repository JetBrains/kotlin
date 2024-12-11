/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.serialization

import com.google.gson.GsonBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.ATTRIBUTES
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENTS
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.FRAGMENT_IDENTIFIER
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib.Companion.UMANIFEST_VERSION
import org.jetbrains.kotlin.incremental.deleteDirectoryContents
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal fun Uklib.serializeToZipArchive(
    outputZip: File,
    temporariesDirectory: File,
) {
    val manifest = GsonBuilder().setPrettyPrinting().create().toJson(
        mapOf(
            FRAGMENTS to module.fragments.sortedBy {
                // Make sure we have some stable order of fragments
                it.identifier
            }.map {
                mapOf(
                    FRAGMENT_IDENTIFIER to it.identifier,
                    ATTRIBUTES to it.attributes
                        // Make sure we have some stable order of attributes
                        .sorted(),
                )
            },
            UMANIFEST_VERSION to manifestVersion,
        )
    )
    zipUklibContents(
        manifest = manifest,
        fragmentToArtifact = module.fragments.map {
            it.identifier to it.file()
        }.toMap(),
        outputZip = outputZip,
        temporariesDirectory = temporariesDirectory,
    )
}

internal data class MissingUklibFragmentFile(val file: File) : IllegalStateException("Missing input file $file")
internal data class IncompatibleUklibFragmentFile(val file: File) : IllegalStateException("Trying to pack invalid file $file")

/**
 * // FIXME: Test rezipping in IT. Maybe use Gradle tools to handle unzipping of input files?
 * // FIXME: Use the proper compression algorithm
 *
 * All of the rezipping happens at execution time in ArchiveUklibTask:
 * - if the incoming fragment is a .klib or a .jar, just unzip it and zip into the .uklib zip
 * - if the incoming fragment is a directory, probably it's an unpacked klib, so just copy it
 */
private val allowRepackingArchivesWithExtensions = setOf(
    "klib",
    "jar",
)

private fun zipUklibContents(
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
            if (!file.exists()) {
                throw MissingUklibFragmentFile(file)
            }
            if (file.isDirectory) {
                packDirectory(
                    directory = file,
                    identifier = identifier,
                    zipOutputStream = zipOutputStream,
                )
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
                throw IncompatibleUklibFragmentFile(file)
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