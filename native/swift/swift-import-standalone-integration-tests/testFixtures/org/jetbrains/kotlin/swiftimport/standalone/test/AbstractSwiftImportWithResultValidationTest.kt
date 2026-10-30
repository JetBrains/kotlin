/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.backend.handlers.UpdateTestDataSupport
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.io.path.div

/**
 * Checks Swift Import artifacts of the test module against `golden_result/<Module>/<Module>.{kt,swift,h}`.
 */
@ExtendWith(UpdateTestDataSupport::class)
abstract class AbstractSwiftImportWithResultValidationTest : AbstractSwiftImportTest() {
    protected fun runTest(@TestDataFile testDir: String) {
        val testPathFull = getAbsoluteFile(testDir)
        val output = importSwiftModule(testPathFull)

        val expectedFiles = testPathFull.toPath() / "golden_result" / output.moduleName
        assertAll(
            { TestDataAssertions.assertEqualsToFile(expectedFiles / "${output.moduleName}.kt", output.kotlinSources) },
            { TestDataAssertions.assertEqualsToFile(expectedFiles / "${output.moduleName}.swift", output.swiftBridges) },
            { TestDataAssertions.assertEqualsToFile(expectedFiles / "${output.moduleName}.h", output.cHeaderBridges) },
        )
    }
}
