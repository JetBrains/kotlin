/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.*
import kotlin.test.*

/**
 * Asserts file under [file] path exists and is a regular file.
 */
fun assertFileExists(
    file: File,
    message: String? = null,
) = assertFileExists(
    file.toPath(),
    message
)

fun assertFileExists(
    file: Path,
    message: String? = null,
) {
    assert(file.exists()) {
        message ?: buildString {
            appendLine("File '${file}' does not exist!")
            if (file.parent.exists()) {
                val parentDirEntries = file.parent.listDirectoryEntries()
                appendLine("Parent directory has ${parentDirEntries.size} entries:")
                appendLine(
                    parentDirEntries.joinToString("\n") {
                        if (it.isDirectory()) {
                            " - ${it.name}/"
                        } else {
                            " - ${it.name}"
                        }
                    }
                )
            } else {
                appendLine("Parent directory '${file.parent}' does not exist.")
            }
        }
    }

    assert(file.isRegularFile()) {
        "File '${file}' exists, but it is not a regular file!"
    }
}

/**
 * Asserts file under [pathToFile] relative to the test project exists and is a regular file.
 */
fun GradleProject.assertFileInProjectExists(
    pathToFile: String,
) {
    assertFileExists(projectPath.resolve(pathToFile))
}

fun assertFileExistsInTree(
    pathToTreeRoot: Path,
    fileName: String,
) {
    val foundFile = pathToTreeRoot
        .toFile()
        .walk()
        .find {
            it.isFile && it.name == fileName
        }

    assert(foundFile != null) {
        "File $fileName does not exists in $pathToTreeRoot!"
    }
}

/**
 * Asserts file under [pathToFile] relative to the test project does not exist.
 */
fun GradleProject.assertFileInProjectNotExists(
    pathToFile: String,
) {
    assertFileNotExists(projectPath.resolve(pathToFile))
}

fun assertFileNotExists(
    pathToFile: Path,
    message: String = "File '${pathToFile}' exists!",
) {
    assert(!Files.exists(pathToFile)) {
        message
    }
}

fun assertFileNotExistsInTree(
    pathToTreeRoot: Path,
    fileName: String,
) {
    val foundFile = pathToTreeRoot
        .toFile()
        .walk()
        .find {
            it.isFile && it.name == fileName
        }

    assert(foundFile == null) {
        "File exists: ${foundFile!!.absolutePath}"
    }
}

fun GradleProject.assertFileNotExistsInTree(
    pathToTreeRoot: String,
    fileName: String,
) {
    assertFileNotExistsInTree(projectPath.resolve(pathToTreeRoot), fileName)
}

/**
 * Asserts symlink under [path] exists and is a symlink
 */
fun assertSymlinkExists(
    path: Path,
) {
    assert(Files.exists(path)) {
        "Symlink '${path}' does not exist!"
    }

    assert(Files.isSymbolicLink(path)) {
        "'${path}' is not a symlink!"
    }
}

/**
 * Asserts symlink under [pathToFile] relative to the test project exists and is a symlink.
 */
fun TestProject.assertSymlinkInProjectExists(
    pathToFile: String,
) {
    assertSymlinkExists(projectPath.resolve(pathToFile))
}

/**
 * Asserts symlink under [pathToFile] relative to the test project exists and points to [pathToPointee].
 */
fun TestProject.assertSymlinkInProjectPointsToProjectPath(
    pathToSymlink: String,
    pathToPointee: String,
) {
    val pointeePath = projectPath.resolve(pathToPointee).toRealPath().toFile().absoluteFile
    val symlinkPath = projectPath.resolve(pathToSymlink)
    val symlinkPointeePath = symlinkPath.toRealPath().toFile().absoluteFile
    assertSymlinkExists(symlinkPath)
    assertEquals(
        pointeePath,
        symlinkPointeePath,
        "Symlink '${symlinkPointeePath}' points to '${symlinkPointeePath}', but was expected to point to '${pointeePath}'"
    )
}

/**
 * Asserts directory under [pathToDir] relative to the test project exists and is a directory.
 */
fun GradleProject.assertDirectoryInProjectExists(
    pathToDir: String,
) = assertDirectoryExists(projectPath.resolve(pathToDir))

/**
 * Asserts directory under [dirPath] exists and is a directory.
 */
fun assertDirectoryExists(
    dirPath: Path,
    message: String? = null,
) = assertDirectoriesExist(dirPath, message = message)

fun assertDirectoriesExist(
    vararg dirPaths: Path,
    message: String? = null,
) {
    val (exist, notExist) = dirPaths.partition { it.exists() }
    val notDirectories = exist.filterNot { it.isDirectory() }

    assert(notExist.isEmpty() && notDirectories.isEmpty()) {
        message ?: buildString {
            if (notExist.isNotEmpty()) {
                appendLine("Following directories does not exist:")
                appendLine(notExist.joinToString(separator = "\n"))
            }
            if (notDirectories.isNotEmpty()) {
                appendLine("Following files should be directories:")
                appendLine(notExist.joinToString(separator = "\n"))
            }
        }
    }
}

private const val appendIndentationIncrement = 2U
private fun StringBuilder.appendDirectory(dirPath: Path, indentation: UInt = 0U) {
    Files.newDirectoryStream(dirPath).use { stream ->
        for (entry in stream) {
            append("${"–".repeat(indentation.toInt())} ${entry.fileName}")
            val isDirectory = Files.isDirectory(entry)
            appendLine(if (isDirectory) " \\" else " (file)")
            if (isDirectory) {
                appendDirectory(entry, indentation + appendIndentationIncrement)
            }
        }
        appendLine()
    }
}

fun GradleProject.assertDirectoryInProjectDoesNotExist(
    dirName: String,
) {
    assertDirectoryDoesNotExist(projectPath.resolve(dirName))
}

fun assertDirectoryDoesNotExist(
    dirPath: Path,
    message: String? = null,
) {
    assert(!Files.exists(dirPath)) {
        message ?: buildString {
            append("Directory $dirPath is expected to not exist. ")
            if (Files.isDirectory(dirPath)) {
                appendLine("The directory contents: ")
                appendDirectory(dirPath)
            } else {
                append("However, it is not even a directory.")
            }
        }
    }
}

/**
 * Asserts file under [pathToFile] relative to the test project exists and contains all the lines from [expectedText]
 */
fun GradleProject.assertFileInProjectContains(
    pathToFile: String,
    vararg expectedText: String,
    ignoreWhitespace: Boolean = false,
) {
    assertFileContains(
        file = projectPath.resolve(pathToFile),
        expectedText = expectedText,
        ignoreWhitespace = ignoreWhitespace
    )
}

/**
 * Asserts file under [pathToFile] relative to the test project exists and does not contain any line from [unexpectedText]
 */
fun GradleProject.assertFileInProjectDoesNotContain(
    pathToFile: String,
    vararg unexpectedText: String,
) {
    assertFileDoesNotContain(projectPath.resolve(pathToFile), *unexpectedText)
}

/**
 * Asserts file under [file] exists and contains all the lines from [expectedText]
 *
 * @param[ignoreWhitespace] will remove all whitespace from [file] content.
 * @return the content of the [file]
 */
fun assertFileContains(
    file: Path,
    vararg expectedText: String,
    ignoreWhitespace: Boolean = false,
): String {
    return assertFilesCombinedContains(
        files = listOf(file),
        expectedText = expectedText,
        ignoreWhitespace = ignoreWhitespace,
    )
}

/**
 * Asserts files together contains all the lines from [expectedText]
 *
 * @param[ignoreWhitespace] will remove all whitespace from the content of all [files].
 */
fun assertFilesCombinedContains(
    files: List<Path>,
    vararg expectedText: String,
    ignoreWhitespace: Boolean = false,
): String {
    assertTrue(files.isNotEmpty(), "Must have at least one file")
    files.forEach { assertFileExists(it) }

    val text = files.joinToString(separator = "\n") {
        if (ignoreWhitespace) {
            it.readText().filterNot(Char::isWhitespace)
        } else {
            it.readText()
        }
    }

    val textNotInTheFile = expectedText.filterNot { text.contains(it) }
    assert(textNotInTheFile.isEmpty()) {
        buildString {
            if (files.size > 1) {
                appendLine("${files.size} files did not contain expected text:")
            } else {
                appendLine("File did not contain expected text:")
            }
            textNotInTheFile.forEach { appendLine(it) }
            if (ignoreWhitespace) appendLine("\n(ignoreWhitespace is enabled)")
            appendLine("\nActual content:")
            appendLine(text)
            appendLine("\nSearched in files:")
            files.forEach { appendLine(" - $it") }
            appendLine()
        }
    }
    return text
}

/**
 * Asserts file under [file] exists and does not contain any line from [unexpectedText]
 */
fun assertFileDoesNotContain(
    file: Path,
    vararg unexpectedText: String,
    message: String? = null,
) {
    assertFileExists(file)
    val text = file.readText()
    val textInTheFile = unexpectedText.filter { text.contains(it) }
    assert(textInTheFile.isEmpty()) {
        """
        |${message ?: ""}
        |$file contains lines which it should not contain:
        |${textInTheFile.joinToString(separator = "\n")}
        |
        |actual file content:
        |$text
        |
        """.trimMargin().trimStart()
    }
}

fun assertSameFiles(expected: Iterable<Path>, actual: Iterable<Path>, messagePrefix: String) {
    val expectedSet = expected.map { it.toString().normalizePath() }.toSet()
    val actualSet = actual.map { it.toString().normalizePath() }.toSet()
    asserter.assertTrue(lazyMessage = {
        messagePrefix +
                "Actual set does not exactly match expected set.\n" +
                "Expected set: ${expectedSet.sorted().joinToString(", ")}\n" +
                "Actual set: ${actualSet.sorted().joinToString(", ")}\n"
    }, actualSet.size == expectedSet.size && actualSet.containsAll(expectedSet))
}

fun assertContainsFiles(expected: Iterable<Path>, actual: Iterable<Path>, messagePrefix: String) {
    val expectedSet = expected.map { it.toString().normalizePath() }.toSet()
    val actualSet = actual.map { it.toString().normalizePath() }.toSet()
    asserter.assertTrue(lazyMessage = {
        messagePrefix +
                "Actual set does not contain all of expected set.\n" +
                "Expected set: ${expectedSet.sorted().joinToString(", ")}\n" +
                "Actual set: ${actualSet.sorted().joinToString(", ")}\n"
    }, actualSet.containsAll(expectedSet))
}

/**
 * Asserts that the content of two files is equal.
 * @param expected The path to the expected file.
 * @param actual The path to the actual file.
 * @throws AssertionError if the contents of the two files are not equal.
 */
fun assertFilesContentEquals(expected: Path, actual: Path) {
    assertFileExists(expected)
    assertFileExists(actual)
    assertContentEquals(
        expected.readLines().asSequence(),
        actual.readLines().asSequence(),
        "Files content not equal"
    )
}

class GradleVariantAssertions(
    val variantJson: JsonObject,
) {
    fun assertAttributesEquals(expected: Map<String, String>) {
        val attributesJson = variantJson.getAsJsonObject("attributes")
        val actual = attributesJson.keySet().associateWith { attributesJson.get(it).asString }

        assertEquals(expected.toSortedStringWithLines(), actual.toSortedStringWithLines())
    }

    fun assertAttributesEquals(vararg expected: Pair<String, String>) = assertAttributesEquals(expected.toMap())
}

private fun Map<String, Any?>.toSortedStringWithLines() = entries
    .sortedBy { it.key }
    .joinToString("\n") { (key, value) -> "'$key' => '$value'" }

fun assertGradleVariant(gradleModuleFile: Path, variantName: String, code: GradleVariantAssertions.() -> Unit) {
    val moduleJson = JsonParser.parseString(gradleModuleFile.readText()).asJsonObject
    val variants = moduleJson.getAsJsonArray("variants")
    val variantJson = variants.find { it.asJsonObject.get("name").asString == variantName }

    if (variantJson == null) {
        val existingVariants = variants.map { it.asJsonObject.get("name").asString }
        throw AssertionError("Variant with name '$variantName' doesn't exist; Existing variants: $existingVariants")
    }

    GradleVariantAssertions(variantJson.asJsonObject).apply(code)
}

fun Path.assertZipArchiveContainsFilesOnce(
    fileNames: List<String>,
) {
    ZipFile(toFile()).use { zip ->
        fileNames.forEach { fileName ->
            assert(zip.entries().asSequence().count { it.name == fileName } == 1) {
                "The jar should contain one entry `$fileName` with no duplicates\n" +
                        zip.entries().asSequence().map { it.name }.joinToString()
            }
        }
    }
}

fun Path.allZipEntries() = ZipFile(toFile()).use { zip -> zip.entries().asSequence().map { it.name }.toSet() }

fun Path.assertZipFileContains(entries: Iterable<String>) {
    assertFileExists(this)
    val actualEntries = allZipEntries()
    val missingEntries = entries - actualEntries
    if (missingEntries.isEmpty()) return
    val failureMessage = buildString {
        appendLine("Following entries are missing in '${this@assertZipFileContains}'")
        missingEntries.forEach { appendLine("  * $it") }
        appendLine()
        appendLine("Actual entries:")
        actualEntries.forEach { appendLine("  * $it") }
    }
    fail(failureMessage)
}
