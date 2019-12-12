/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import org.hamcrest.core.Is
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert.assertThat
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

internal fun Path.readFile() = toFile().readText().trim()

internal fun Path.listFiles(filter: (Path) -> Boolean) =
    Files.walk(this).filter { path ->
        Files.isRegularFile(path) && filter(path)
    }.collect(Collectors.toList()).sorted()

internal fun compareFiles(
    expectedFiles: List<Path>, expectedDir: Path,
    actualFiles: List<Path>, actualDir: Path
) {
    val expectedFilesSorted = expectedFiles.sorted()
    val actualFilesSorted = actualFiles.sorted()

    assertThat(
        actualFilesSorted.map { actualDir.relativize(it) },
        Is.`is`(expectedFilesSorted.map { expectedDir.relativize(it) })
    )

    for ((actualFile, expectedFile) in actualFilesSorted zip expectedFilesSorted) {
        KotlinTestUtils.assertEqualsToFile(expectedFile.toFile(), actualFile.readFile())
    }
}