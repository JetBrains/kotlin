/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.Deflater

// TODO KAR: Split into reasonable parts and cleanup
@DisableCachingByDefault(because = "Packing a KAR archive is not worth caching")
internal abstract class PackKarTask : AbstractArchiveTask() {
    override fun createCopyAction(): CopyAction = PackKarCopyAction(
        archiveFile = archiveFile.get().asFile,
    )
}

private class PackKarCopyAction(
    private val archiveFile: File,
) : CopyAction {
    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        try {
            archiveFile.outputStream().buffered().use { output ->
                XZCompressorOutputStream(output).use { compressedOutput ->
                    ZipArchiveOutputStream(compressedOutput).use { zipOutput ->
                        /**
                         * We don't need compression here, as compression would be handled by outer xz layer.
                         * But we also can't use STORED method zip file, as to write it to stream, which doesn't support
                         * seek (such as XZCompressorOutputStream used here) we need to compute files crc's manually
                         * in advance. That's both less convinient and slower. So we use DEFLATED method wihtout compression.
                         */
                        zipOutput.setLevel(Deflater.NO_COMPRESSION)
                        /**
                         * We want to put files with the same name nearby each other in archive.
                         * Files with the same name would correspoind to platform-specific version of the same thing.
                         * In most cases, they should be very similar to each other, so they would be compressed much better,
                         * if located nearby.
                         *
                         * On kotlinx-coroutines-core, it improves compression by extra 10%.
                         * This difference should be larger as total klib size grouth.
                         */
                        val detailsList = buildList<FileCopyDetails> {
                            stream.process { details -> add(details) }
                            sortBy { it.name }
                        }
                        for (details in detailsList) {
                            val entry = ZipArchiveEntry(
                                details.relativePath.pathString + if (details.isDirectory) "/" else ""
                            ).apply {
                                time = 0
                            }

                            zipOutput.putArchiveEntry(entry)
                            if (!details.isDirectory) details.copyTo(zipOutput)
                            zipOutput.closeArchiveEntry()
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            archiveFile.delete()
            throw exception
        }

        return WorkResults.didWork(true)
    }
}


abstract class XZDecompressAction : TransformAction<TransformParameters.None> {

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    abstract fun getArchive(): Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val archiveFile = getArchive().get().asFile
        val targetFile = outputs.file(archiveFile.nameWithoutExtension)

        archiveFile.inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                targetFile.outputStream().buffered().use { fileOutput ->
                    xzInput.copyTo(fileOutput)
                }
            }
        }
    }
}
