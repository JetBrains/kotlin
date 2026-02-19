/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

class CompilerDistContentsTest {
    private val kotlincPath = System.getProperty("kotlinc.dist.path").let { Paths.get(it) }

    private val expectedDataFile = File("repo/artifacts-tests/src/test/resources/compiler-dist-contents.txt")

    @Test
    fun checkDistContents() {
        assertEqualsToFile(expectedDataFile, getActualContent().sorted().joinToString("\n"))
    }

    private fun getActualContent() = Files.find(
        kotlincPath,
        Integer.MAX_VALUE,
        { path: Path, fileAttributes: BasicFileAttributes -> fileAttributes.isRegularFile }
    ).asSequence().map { it.toAbsolutePath().relativeTo(kotlincPath).toString().replace("\\", "/") }
}