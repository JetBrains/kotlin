/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.tests

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.StubRenderer
import org.jetbrains.kotlin.backend.konan.testUtils.baseDeclarationsDir
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ## Test Scope
 * This test will just invoke the generation of 'base declarations' (basically ObjC stubs that shall always be present)
 * The generated declarations will be compared to previously checked-in 'golden' headers.
 */
class ObjCExportBaseDeclarationsTest(
    /**
     * Injected implementation: Either K1 or based upon Analysis API.
     */
    private val generator: BaseDeclarationsGenerator,
) {

    @Test
    fun `test - noTopLevelPrefix`() {
        doTest(baseDeclarationsDir.resolve("!noTopLevelPrefix.h"), "")
    }

    @Test
    fun `test - topLevelPrefix`() {
        doTest(baseDeclarationsDir.resolve("!topLevelPrefix.h"), "MyTopLevelPrefix")
    }

    private fun doTest(headerFile: File, topLevelPrefix: String) {
        val declarations = generator(topLevelPrefix)

        val renderedDeclarations = declarations
            .flatMap { declaration -> StubRenderer.render(declaration) }
            .joinToString(System.lineSeparator())

        KotlinTestUtils.assertEqualsToFile(headerFile, renderedDeclarations)
    }

    fun interface BaseDeclarationsGenerator {
        operator fun invoke(topLevelPrefix: String): List<ObjCTopLevel>
    }
}
