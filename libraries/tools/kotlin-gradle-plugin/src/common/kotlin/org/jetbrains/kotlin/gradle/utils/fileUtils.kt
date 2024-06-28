/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.internal.CustomPropertiesFileValueSource
import org.jetbrains.kotlin.gradle.plugin.internal.configurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.usedAtConfigurationTime
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Create all possible case-sensitive permutations for given [String].
 *
 * Useful to create for [org.gradle.api.tasks.util.PatternFilterable] Ant-style patterns.
 */
@OptIn(ExperimentalStdlibApi::class)
internal fun String.fileExtensionCasePermutations(): List<String> {
    val lowercaseInput = lowercase()
    val length = lowercaseInput.length
    // number of permutations is 2^n
    val max = 1 shl length
    val result = mutableListOf<String>()
    var combination: CharArray
    for (i in 0 until max) {
        combination = lowercaseInput.toCharArray()
        for (j in 0 until length) {
            // If j-th bit is set, we convert it to upper case
            if (((i shr j) and 1) == 1) {
                combination[j] = combination[j].uppercaseChar()
            }
        }
        result.add(String(combination))
    }
    return result
}

internal fun File.relativeOrAbsolute(base: File): String =
    relativeToOrNull(base)?.path ?: normalize().absolutePath

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
    map { it.relativeOrAbsolute(base) }.sorted().joinToString()

internal fun Iterable<File>.toPathsArray(): Array<String> =
    map { it.normalize().absolutePath }.toTypedArray()

internal fun newTmpFile(prefix: String, suffix: String? = null, directory: File? = null, deleteOnExit: Boolean = true): File {
    return try {
        (if (directory == null) Files.createTempFile(prefix, suffix) else Files.createTempFile(directory.toPath(), prefix, suffix))
    } catch (e: NoSuchFileException) {
        val parentDir = e.file.parentFile

        if (parentDir.isFile) throw IOException("Temp folder $parentDir is not a directory")
        if (!parentDir.isDirectory) {
            if (!parentDir.mkdirs()) throw IOException("Could not create temp directory $parentDir")
        }

        Files.createTempFile(parentDir.toPath(), prefix, suffix)
    }.toFile().apply { if (deleteOnExit) deleteOnExit() }
}

internal fun File.isParentOf(childCandidate: File, strict: Boolean = false): Boolean {
    val parentPath = Paths.get(this.absolutePath).normalize()
    val childCandidatePath = Paths.get(childCandidate.absolutePath).normalize()

    return if (strict) {
        childCandidatePath.startsWith(parentPath) && parentPath != childCandidate
    } else {
        childCandidatePath.startsWith(parentPath)
    }
}

internal fun File.absolutePathWithoutExtension(): String =
    normalize().absolutePath.substringBeforeLast(".")

internal fun File.listFilesOrEmpty() = (if (exists()) listFiles() else null).orEmpty()

fun contentEquals(file1: File, file2: File): Boolean {
    file1.useLines { seq1 ->
        file2.useLines { seq2 ->
            val iterator1 = seq1.iterator()
            val iterator2 = seq2.iterator()

            while (iterator1.hasNext() == iterator2.hasNext()) {

                if (!iterator1.hasNext()) return true

                if (iterator1.next() != iterator2.next()) {
                    return false
                }
            }

            return true
        }
    }
}

internal fun RegularFile.toUri() = asFile.toPath().toUri()

internal fun Provider<RegularFile>.mapToFile(): Provider<File> = map { it.asFile }

@JvmName("mapDirectoryToFile") // avoids jvm signature clash
internal fun Provider<Directory>.mapToFile(): Provider<File> = map { it.asFile }

internal fun Provider<RegularFile>.getFile(): File = get().asFile

@JvmName("getDirectoryAsFile") // avoids jvm signature clash
internal fun Provider<Directory>.getFile(): File = get().asFile

/**
 * Checks if the file exists, taking into account compatibility with different versions of Gradle.
 * It should be used instead of [File.exists] in checking UPD inputs. See KT-54232 for more info.
 *
 * @return `true` if the file exists, `false` otherwise.
 *
 * NOTE: You can remove this method and all its usages since the minimal supported version of gradle become 8.0
 */
internal fun File.existsCompat(): Boolean =
    if (isGradleVersionAtLeast(8, 0)) {
        true
    } else {
        exists()
    }

/**
 * Loads 'local.properties' file content as [Properties].
 *
 * If it does not exist, returned [Provider] will be empty.
 */
internal val Project.localProperties: Provider<Map<String, String>>
    get() = providers
        .of(CustomPropertiesFileValueSource::class.java) {
            it.parameters.propertiesFile.set(
                project.rootDir.resolve("local.properties")
            )
        }
        .usedAtConfigurationTime(configurationTimePropertiesAccessor)

/**
 * Returns file collection [this] excluding files from [excludes] if not null
 */
internal fun FileCollection.exclude(excludes: FileCollection?): FileCollection = if (excludes != null) minus(excludes) else this

internal fun Project.fileCollectionFromConfigurableFileTree(fileTree: ConfigurableFileTree): ConfigurableFileCollection {
    // It is important to pass exactly `fileTree.dir` as provider with explicit task dependency
    // Because of the following bugs:
    // * https://github.com/gradle/gradle/issues/27881 ConfigurableFileTree.from() doesn't preserve Task Dependencies
    // * https://github.com/gradle/gradle/issues/27882 SourceDirectorySet doesn't accept ConfigurableFileTree
    return project.filesProvider(fileTree) { fileTree.dir }
}