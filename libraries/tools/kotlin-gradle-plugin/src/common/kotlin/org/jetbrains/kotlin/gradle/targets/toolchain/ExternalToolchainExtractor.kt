/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * A downloaded [archive] of [toolchain], together with the Gradle services needed to extract it.
 */
internal class ToolchainExtractionContext<out T : ExternalToolchainForKotlin>(
    val toolchain: T,
    val archive: File,
    val fs: FileSystemOperations,
    val archiveOperations: ArchiveOperations,
)

/**
 * Extracts the toolchain from a downloaded archive, and stores it in the desired place.
 */
internal fun interface ExternalToolchainExtractor<in T : ExternalToolchainForKotlin> {

    /**
     * Extracts the toolchain from [ToolchainExtractionContext.archive].
     *
     * On success, [destinationDir] must be a complete installation of [ToolchainExtractionContext.toolchain].
     */
    fun extract(context: ToolchainExtractionContext<T>, destinationDir: File)
}

/**
 * Unpacks an archive of a specific format into a directory, as is.
 */
internal fun interface ToolchainArchiveExtractor<in T : ExternalToolchainForKotlin> {
    fun unpack(context: ToolchainExtractionContext<T>, destinationDir: File)
}

internal object ZipToolchainExtractor : ToolchainArchiveExtractor<ExternalToolchainForKotlin> {
    override fun unpack(context: ToolchainExtractionContext<ExternalToolchainForKotlin>, destinationDir: File) {
        context.copyTree(context.archiveOperations.zipTree(context.archive), destinationDir)
    }
}

internal object TarGzToolchainExtractor : ToolchainArchiveExtractor<ExternalToolchainForKotlin> {
    override fun unpack(context: ToolchainExtractionContext<ExternalToolchainForKotlin>, destinationDir: File) {
        context.copyTree(context.archiveOperations.tarTree(context.archive), destinationDir)
    }
}

/**
 * Gradle cannot read `.tar.xz` archives directly, see https://github.com/gradle/gradle/issues/31858,
 * so the archive is decompressed into a plain `.tar` first.
 */
internal object TarXzToolchainExtractor : ToolchainArchiveExtractor<ExternalToolchainForKotlin> {
    override fun unpack(context: ToolchainExtractionContext<ExternalToolchainForKotlin>, destinationDir: File) {
        val archive = context.archive
        val tarFile = archive.resolveSibling("${archive.nameWithoutExtension}.tar")
        XZCompressorInputStream(archive.inputStream().buffered()).use { xzIn ->
            tarFile.outputStream().buffered().use { tarOut -> xzIn.copyTo(tarOut) }
        }
        context.copyTree(context.archiveOperations.tarTree(tarFile), destinationDir)
    }
}

/**
 * The distribution is a single executable file, published without any archiving, so it is moved
 * to the [ExternalToolchainForKotlin.executablePath] as is.
 */
internal object RawExecutableToolchainExtractor : ToolchainArchiveExtractor<ExternalToolchainForKotlin> {
    override fun unpack(context: ToolchainExtractionContext<ExternalToolchainForKotlin>, destinationDir: File) {
        val executable = destinationDir.resolve(context.toolchain.executablePath)
        executable.parentFile.mkdirs()
        Files.move(context.archive.toPath(), executable.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

/**
 * Selects the [ToolchainArchiveExtractor] by the context, for example when the archive format depends on the platform.
 */
internal class CombinedToolchainExtractor<in T : ExternalToolchainForKotlin>(
    private val select: (ToolchainExtractionContext<T>) -> ToolchainArchiveExtractor<T>,
) : ToolchainArchiveExtractor<T> {
    override fun unpack(context: ToolchainExtractionContext<T>, destinationDir: File) {
        select(context).unpack(context, destinationDir)
    }
}

/**
 * Unpacks the archive with [archiveExtractor], verifies that it contains the tool executable,
 * and moves the distribution into the destination directory.
 *
 * @param archiveRootDir the directory inside the archive that holds the distribution, or `null` when the archive
 * content itself is the distribution.
 */
internal class DefaultExternalToolchainExtractor<in T : ExternalToolchainForKotlin>(
    private val archiveExtractor: ToolchainArchiveExtractor<T>,
    private val archiveRootDir: (T) -> String? = { null },
) : ExternalToolchainExtractor<T> {

    override fun extract(context: ToolchainExtractionContext<T>, destinationDir: File) {
        val toolchain = context.toolchain
        val unpackDir = context.archive.resolveSibling(UNPACK_DIR_NAME)
        archiveExtractor.unpack(context, unpackDir)

        val root = archiveRootDir(toolchain)?.let { rootDirName ->
            unpackDir.resolve(rootDirName).also {
                check(it.isDirectory) {
                    "The ${toolchain.toolName} distribution archive '${context.archive.name}' does not contain the expected " +
                            "'$rootDirName' directory"
                }
            }
        } ?: unpackDir

        val executable = root.resolve(toolchain.executablePath)
        check(executable.isFile) {
            "The ${toolchain.toolName} distribution archive '${context.archive.name}' does not contain the " +
                    "'${toolchain.executablePath}' executable"
        }
        if (!toolchain.platform.isWindows) {
            executable.setExecutable(true)
        }

        destinationDir.parentFile.mkdirs()
        Files.move(root.toPath(), destinationDir.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private companion object {
        private const val UNPACK_DIR_NAME = "unpacked"
    }
}

private fun ToolchainExtractionContext<*>.copyTree(tree: Any, destinationDir: File) {
    fs.copy {
        it.from(tree)
        it.into(destinationDir)
    }
}
