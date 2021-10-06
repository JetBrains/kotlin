/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import java.nio.file.Files
import java.nio.file.Path

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

/**
 * Asserts file under [pathToFile] relative to the test project does not exist.
 */
fun GradleProject.assertFileNotExists(
    pathToFile: String
) {
    val filePath = projectPath.resolve(pathToFile)
    assert(!Files.exists(filePath)) {
        "File '${filePath}' exists!"
    }
}

/**
 * Asserts directory under [pathToDir] relative to the test project exists and is a directory.
 */
fun GradleProject.assertDirectoryExists(
    pathToDir: String
) {
    val dirPath = projectPath.resolve(pathToDir)

    assert(Files.exists(dirPath)) {
        "Directory '${dirPath}' does not exist!"
    }

    assert(Files.isDirectory(dirPath)) {
        "'${dirPath}' is not a directory!"
    }
}