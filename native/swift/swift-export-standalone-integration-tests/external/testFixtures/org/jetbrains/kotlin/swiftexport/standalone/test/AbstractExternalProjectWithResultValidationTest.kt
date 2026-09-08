/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.swiftexport.standalone.SwiftExportModule
import java.io.File

abstract class AbstractExternalProjectWithResultValidationTest : AbstractExternalProjectTest(), SwiftExportValidator {
    override fun runTest(
        modules: Set<TestModule.Given>,
        testPathFull: File,
        swiftExportOutputs: Set<SwiftExportModule>,
    ) {
        validateSwiftExportOutput(testPathFull, swiftExportOutputs)
    }
}
