/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile

abstract class AbstractSwiftExportWithResultValidationTest : AbstractSwiftExportTest(), SwiftExportValidator {
    protected fun runTest(@TestDataFile testDir: String) {
        val (swiftExportOutputs) = runConvertToSwift(testDir)
        val testPathFull = getAbsoluteFile(testDir)

        validateSwiftExportOutput(testPathFull, swiftExportOutputs)
    }
}
