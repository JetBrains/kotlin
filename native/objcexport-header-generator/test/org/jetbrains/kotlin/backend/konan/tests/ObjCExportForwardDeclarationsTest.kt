/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.forwardDeclarationsDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

class ObjCExportForwardDeclarationsTest(
    private val generator: HeaderGenerator,
) {

    @Test
    fun `test - function returning interface`() {
        doTest(forwardDeclarationsDir.resolve("functionReturningInterface"))
    }

    @Test
    fun `test - function returning class`() {
        doTest(forwardDeclarationsDir.resolve("functionReturningClass"))
    }

    @Test
    fun `test - property returning interface`() {
        doTest(forwardDeclarationsDir.resolve("propertyReturningInterface"))
    }

    @Test
    fun `test - property returning class`() {
        doTest(forwardDeclarationsDir.resolve("propertyReturningClass"))
    }

    private fun doTest(root: File) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root)
        val renderedForwardDeclarations = buildString {
            generatedHeaders.renderClassForwardDeclarations().forEach(this::appendLine)
            generatedHeaders.renderProtocolForwardDeclarations().forEach(this::appendLine)
        }
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), renderedForwardDeclarations)
    }
}