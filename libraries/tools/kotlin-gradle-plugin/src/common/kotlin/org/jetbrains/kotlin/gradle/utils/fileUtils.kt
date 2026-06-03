/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.internal.CustomPropertiesFileValueSource
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

/**
 * Create all possible case-sensitive permutations for given [String].
 *
 * Useful to create for [org.gradle.api.tasks.util.PatternFilterable] Ant-style patterns.
 */
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
    toPath().relativeOrAbsolute(base.toPath())

internal fun Path.relativeOrAbsolute(base: Path): String =
    try {
        base.relativize(this).toString()
    } catch (_: IllegalArgumentException) {
        toAbsolutePath().normalize().toString()
    }

/**
 * Returns an absolute path with `.` and `..` segments resolved lexically, without filesystem I/O.
 *
 * Use at configuration time or when the path may not exist on disk.
 * Does not resolve symlinks. Avoids [java.io.File.getCanonicalFile] (banned per KT-69613).
 */
internal fun File.normalizedAbsoluteFile(): File =
    toPath().normalizedAbsolutePath().toFile()

internal fun Path.normalizedAbsolutePath(): Path =
    toAbsolutePath().normalize()

internal fun Iterable<File>.pathsAsStringRelativeTo(base: File): String =
    map { it.toPath() }.pathsAsStringRelativeTo(base.toPath())

internal fun Iterable<Path>.pathsAsStringRelativeTo(base: Path): String =
    map { it.relativeOrAbsolute(base) }.sorted().joinToString()

internal fun Iterable<File>.toPathsArray(): Array<String> =
    map { it.toPath() }.toPathStringsArray()

internal fun Iterable<Path>.toPathStringsArray(): Array<String> =
    map { it.toAbsolutePath().normalize().toString() }.toTypedArray()

internal fun newTmpFile(prefix: String, suffix: String? = null, directory: File? = null, deleteOnExit: Boolean = true): File {
    return newTmpPath(prefix, suffix, directory?.toPath(), deleteOnExit).toFile()
}

internal fun newTmpPath(prefix: String, suffix: String? = null, directory: Path? = null, deleteOnExit: Boolean = true): Path {
    return try {
        if (directory == null) Files.createTempFile(prefix, suffix) else Files.createTempFile(directory, prefix, suffix)
    } catch (e: NoSuchFileException) {
        val parentDir = e.file.toPath().parent

        if (Files.isRegularFile(parentDir)) throw IOException("Temp folder $parentDir is not a directory")
        if (!Files.isDirectory(parentDir)) {
            Files.createDirectories(parentDir)
        }

        Files.createTempFile(parentDir, prefix, suffix)
    }.apply { if (deleteOnExit) toFile().deleteOnExit() }
}

internal fun Path.isParentOf(childCandidate: Path, strict: Boolean = false): Boolean {
    val parentPath = toAbsolutePath().normalize()
    val childCandidatePath = childCandidate.toAbsolutePath().normalize()

    return if (strict) {
        childCandidatePath.startsWith(parentPath) && parentPath != childCandidatePath
    } else {
        childCandidatePath.startsWith(parentPath)
    }
}

internal fun File.listFilesOrEmpty() = (if (exists()) listFiles() else null).orEmpty()

@Deprecated("Internal KGP utility. Scheduled for removal in Kotlin 2.7.")
fun contentEquals(file1: File, file2: File): Boolean {
    return contentEqualsIgnoringLineEndings(file1.toPath(), file2.toPath())
}

internal fun contentEqualsIgnoringLineEndings(file1: Path, file2: Path): Boolean {
    Files.newBufferedReader(file1).useLines { seq1 ->
        Files.newBufferedReader(file2).useLines { seq2 ->
            val iterator1 = seq1.iterator()
            val iterator2 = seq2.iterator()

            while (iterator1.hasNext() && iterator2.hasNext()) {
                if (iterator1.next() != iterator2.next()) return false
            }

            return when {
                iterator1.hasNext() -> false
                iterator2.hasNext() -> false
                else -> true
            }
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
    if (GradleVersion.current() >= GradleVersion.version("8.0")) {
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

// copied from IJ
internal fun getJdkClassesRoots(home: Path, isJre: Boolean): List<File> {
    return getJdkClassesRootPaths(home, isJre).map { it.toFile() }
}

internal fun getJdkClassesRootPaths(home: Path, isJre: Boolean): List<Path> {
    val jarDirs: Array<Path>
    val fileName = home.fileName
    if (fileName != null && "Home" == fileName.toString() && Files.exists(home.resolve("../Classes/classes.jar"))) {
        val libDir = home.resolve("lib")
        val classesDir = home.resolveSibling("Classes")
        val libExtDir = libDir.resolve("ext")
        val libEndorsedDir = libDir.resolve("endorsed")
        jarDirs = arrayOf(libEndorsedDir, libDir, classesDir, libExtDir)
    } else if (Files.exists(home.resolve("lib/jrt-fs.jar"))) {
        jarDirs = emptyArray()
    } else {
        val libDir = home.resolve(if (isJre) "lib" else "jre/lib")
        val libExtDir = libDir.resolve("ext")
        val libEndorsedDir = libDir.resolve("endorsed")
        jarDirs = arrayOf(libEndorsedDir, libDir, libExtDir)
    }

    val rootFiles: MutableList<Path> = ArrayList<Path>()

    val pathFilter: MutableSet<String?> = hashSetOf()
    for (jarDir in jarDirs) {
        if (Files.isDirectory(jarDir)) {
            try {
                Files.newDirectoryStream(jarDir, "*.jar").use { stream ->
                    for (jarFile in stream) {
                        val jarFileName = jarFile.getFileName().toString()
                        if (jarFileName == "alt-rt.jar" || jarFileName == "alt-string.jar") {
                            continue  // filter out alternative implementations
                        }
                        val canonicalPath = jarFile.toRealPath().toString()
                        if (!pathFilter.add(canonicalPath)) {
                            continue  // filter out duplicate (symbolically linked) .jar files commonly found in OS X JDK distributions
                        }
                        rootFiles.add(jarFile)
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    if (rootFiles.any { path -> path.fileName.toString().startsWith("ibm") }) {
        // ancient IBM JDKs split JRE classes between `rt.jar` and `vm.jar`, and the latter might be anywhere
        try {
            Files.walk(if (isJre) home else home.resolve("jre")).use { paths ->
                paths.filter { path: Path? -> path!!.fileName.toString() == "vm.jar" }
                    .findFirst()
                    .ifPresent(Consumer { e -> rootFiles.add(e) })
            }
        } catch (_: IOException) {
        }
    }

    val classesZip = home.resolve("lib/classes.zip")
    if (Files.isRegularFile(classesZip)) {
        rootFiles.add(classesZip)
    }

    if (rootFiles.isEmpty()) {
        val classesDir = home.resolve("classes")
        if (Files.isDirectory(classesDir)) {
            rootFiles.add(classesDir)
        }
    }

    return rootFiles
}

internal val FileCollection.onlyJars: FileCollection get() = filter { it.extension == "jar" }

// stdlib use function adapted AutoClosable
internal inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (_: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this?.close()
        }
    }
}

// stdlib 'invariantSeparatorsPathString' copied for 'Path'
internal val Path.invariantSeparatorsPathString: String
    get() {
        val separator = fileSystem.separator
        return if (separator != "/") toString().replace(separator, "/") else toString()
    }
