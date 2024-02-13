/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.testUtils.HeaderGenerator
import org.jetbrains.kotlin.backend.konan.testUtils.TodoAnalysisApi
import org.jetbrains.kotlin.backend.konan.testUtils.dependenciesDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.fail

/**
 * Currently analysis api doesn't fails until full support of stable order and other parts
 *
 * Test intended to verify how dependencies are translated, which included
 * - stable orders
 * - stubs orders
 * - depth of traversing (some types must be skipped
 */
class ObjCDependenciesTypesTest(
    private val generator: HeaderGenerator,
) {

    /**
     * - Missing implementation of mangling
     */
    @Test
    @TodoAnalysisApi
    fun `test - stringBuilder`() {
        doTest(dependenciesDir.resolve("stringBuilder"))
    }

    @Test
    fun `test - iterator`() {
        doTest(dependenciesDir.resolve("iterator"))
    }

    @Test
    fun `test - array`() {
        doTest(dependenciesDir.resolve("array"))
    }

    @Test
    fun `test - arrayList`() {
        doTest(dependenciesDir.resolve("arrayList"))
    }

    /**
     * Int* type conversion issue: KT-65687
     */
    @Test
    @TodoAnalysisApi
    fun `test - implementIterator`() {
        doTest(dependenciesDir.resolve("implementIterator"))
    }

    private fun doTest(root: File) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(root, HeaderGenerator.Configuration()).toString()
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}