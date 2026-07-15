/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import com.android.tools.r8.internal.xz
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.Deflater

@DisableCachingByDefault(because = "Packing a KAR archive is not worth caching")
internal abstract class PackKarTask : AbstractArchiveTask() {
    override fun createCopyAction(): CopyAction = PackKarCopyAction(
        archiveFile.get().asFile
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
                        zipOutput.setLevel(Deflater.NO_COMPRESSION)
                        stream.process(PackKarStreamAction(zipOutput))
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

private class PackKarStreamAction(
    private val zipOutput: ZipArchiveOutputStream,
) : CopyActionProcessingStreamAction {
    override fun processFile(details: FileCopyDetailsInternal) {
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


abstract class XZDecompressAction : TransformAction<TransformParameters.None> {

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    abstract fun getArchive(): Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val archiveFile = getArchive().get().asFile
        val targetFile = outputs.file(archiveFile.nameWithoutExtension + ".kar")

        archiveFile.inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                targetFile.outputStream().buffered().use { fileOutput ->
                    xzInput.copyTo(fileOutput)
                }
            }
        }

        println(targetFile.name + ": written!")
    }
}
