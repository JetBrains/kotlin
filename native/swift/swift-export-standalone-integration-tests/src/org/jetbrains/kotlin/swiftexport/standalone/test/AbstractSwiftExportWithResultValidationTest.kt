/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.getAbsoluteFile
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.junit.jupiter.api.Assumptions
import kotlin.io.path.div

abstract class AbstractSwiftExportWithResultValidationTest : AbstractSwiftExportTest(), SwiftExportValidator {
    private object Directives : SimpleDirectivesContainer() {
        // TODO: better refactor to new test infrastructure
        val APPLE_ONLY_VALIDATION by stringDirective("Ignore swift export with result validation test for non-apple platforms")
    }

    protected fun runTest(@TestDataFile testDir: String) {
        val testPathFull = getAbsoluteFile(testDir)
        val mainFile = (testPathFull.toPath() / "${testPathFull.name}.kt").toFile()
        val onlyApple = "// ${Directives.APPLE_ONLY_VALIDATION.name}" in mainFile.readLines()
        Assumptions.assumeTrue(!onlyApple || testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
        Assumptions.assumeFalse(isTestIgnored(testDir))

        val (swiftExportOutputs) = runConvertToSwift(testDir)
        validateSwiftExportOutput(testPathFull, swiftExportOutputs)
    }
}
