/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.test.assertEquals

/**
 * Asserts file under [file] path exists and is a regular file.
 */
fun GradleProject.assertFileExists(
    file: Path
) {
    assert(Files.exists(file)) {
        "File '${file}' does not exist!"
    }

    assert(Files.isRegularFile(file)) {
        "'${file}' is not a regular file!"
    }
}

/**
 * Asserts file under [pathToFile] relative to the test project exists and is a regular file.
 */
fun GradleProject.assertFileInProjectExists(
    pathToFile: String
) {
    assertFileExists(projectPath.resolve(pathToFile))
}

fun GradleProject.assertFileExistsInTree(
    pathToTreeRoot: Path,
    fileName: String
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
    pathToFile: String
) {
    assertFileNotExists(projectPath.resolve(pathToFile))
}

fun GradleProject.assertFileNotExists(
    pathToFile: Path
) {
    assert(!Files.exists(pathToFile)) {
        "File '${pathToFile}' exists!"
    }
}

fun GradleProject.assertFileNotExistsInTree(
    pathToTreeRoot: Path,
    fileName: String
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
    fileName: String
) {
    assertFileNotExistsInTree(projectPath.resolve(pathToTreeRoot), fileName)
}

/**
 * Asserts directory under [pathToDir] relative to the test project exists and is a directory.
 */
fun GradleProject.assertDirectoryInProjectExists(
    pathToDir: String
) = assertDirectoryExists(projectPath.resolve(pathToDir))

/**
 * Asserts directory under [file] exists and is a directory.
 */
fun GradleProject.assertDirectoryExists(
    dirPath: Path
) = assertDirectoriesExist(dirPath)

fun GradleProject.assertDirectoriesExist(
    vararg dirPaths: Path
) {
    val (exist, notExist) = dirPaths.partition { it.exists() }
    val notDirectories = exist.filterNot { it.isDirectory() }

    assert(notExist.isEmpty() && notDirectories.isEmpty()) {
        buildString {
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

/**
 * Asserts file under [pathToFile] relative to the test project exists and contains all the lines from [expectedLines]
 */
fun GradleProject.assertFileInProjectContains(
    pathToFile: String,
    vararg expectedText: String
) {
    assertFileContains(projectPath.resolve(pathToFile), *expectedText)
}

/**
 * Asserts file under [pathToFile] relative to the test project exists and does not contain any line from [unexpectedText]
 */
fun GradleProject.assertFileInProjectDoesNotContain(
    pathToFile: String,
    vararg unexpectedText: String
) {
    assertFileDoesNotContain(projectPath.resolve(pathToFile), *unexpectedText)
}

/**
 * Asserts file under [file] exists and contains all the lines from [expectedText]
 */
fun GradleProject.assertFileContains(
    file: Path,
    vararg expectedText: String
) {
    assertFileExists(file)
    val text = file.readText()
    val textNotInTheFile = expectedText.filterNot { text.contains(it) }
    assert(textNotInTheFile.isEmpty()) {
        """
        |$file does not contain:
        |${textNotInTheFile.joinToString(separator = "\n")}
        |
        |actual file content:
        |$text"
        |       
        """.trimMargin()
    }
}

/**
 * Asserts file under [file] exists and does not contain any line from [unexpectedText]
 */
fun GradleProject.assertFileDoesNotContain(
    file: Path,
    vararg unexpectedText: String
) {
    assertFileExists(file)
    val text = file.readText()
    val textInTheFile = unexpectedText.filter { text.contains(it) }
    assert(textInTheFile.isEmpty()) {
        """
        |$file contains lines which it should not contain:
        |${textInTheFile.joinToString(separator = "\n")}
        |
        |actual file content:
        |$text"
        |       
        """.trimMargin()
    }
}

fun GradleProject.assertSameFiles(expected: Iterable<Path>, actual: Iterable<Path>, messagePrefix: String) {
    val expectedSet = expected.map { it.toString().normalizePath() }.toSortedSet().joinToString("\n")
    val actualSet = actual.map { it.toString().normalizePath() }.toSortedSet().joinToString("\n")
    assertEquals(expectedSet, actualSet, messagePrefix)
}