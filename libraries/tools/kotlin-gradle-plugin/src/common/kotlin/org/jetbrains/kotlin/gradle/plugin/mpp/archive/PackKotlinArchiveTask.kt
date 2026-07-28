/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.archive

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.OutputStream
import java.util.zip.Deflater

@DisableCachingByDefault(because = "Packing a Kotlin Archive is not worth caching")
internal abstract class PackKotlinArchiveTask: AbstractArchiveTask() {
    override fun createCopyAction(): CopyAction = PackKotlinArchiveCopyAction(
        archiveFile = archiveFile.get().asFile,
    )
}

private class PackKotlinArchiveCopyAction(
    private val archiveFile: File,
) : CopyAction {

    private fun ZipArchiveOutputStream.putContent(filesList: List<FileCopyDetails>) {
        /**
         * We don't need compression here, as compression would be handled by outer xz layer.
         * But we also can't use STORED method zip file, as to write it to stream, which doesn't support
         * seek (such as XZCompressorOutputStream used here) we need to compute files crc's manually
         * in advance. That's both less convinient and slower. So we use DEFLATED method wihtout compression.
         */
        setLevel(Deflater.NO_COMPRESSION)
        for (file in filesList) {
            val entry = ZipArchiveEntry(
                file.relativePath.pathString + if (file.isDirectory) "/" else ""
            ).apply {
                this.time = 0
            }

            putArchiveEntry(entry)
            if (!file.isDirectory) file.copyTo(this)
            closeArchiveEntry()
        }
    }

    private fun File.writeBufferedAndDeleteOnFailure(block: (OutputStream) -> Unit) {
        try {
            outputStream().buffered().use { block(it) }
        } catch (exception: Exception) {
            delete()
            throw exception
        }
    }

    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        /**
         * We want to put files with the same name nearby each other in archive.
         * Files with the same name would correspoind to platform-specific version of the same thing.
         * In most cases, they should be very similar to each other, so they would be compressed much better,
         * if located nearby.
         *
         * On kotlinx-coroutines-core, it improves compression by extra 10%.
         * This difference should be larger as total klib size grouth.
         */
        val filesList = buildList<FileCopyDetails> {
            stream.process { details -> add(details) }
            sortBy { it.name }
        }

        archiveFile.writeBufferedAndDeleteOnFailure { output ->
            XZCompressorOutputStream(output).use { compressedOutput ->
                ZipArchiveOutputStream(compressedOutput).use { zipOutput ->
                    zipOutput.putContent(filesList)
                }
            }
        }

        return WorkResults.didWork(true)
    }
}
