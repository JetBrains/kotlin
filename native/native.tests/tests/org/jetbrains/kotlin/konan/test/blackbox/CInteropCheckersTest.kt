/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
abstract class BaseCInteropCheckersTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata(TEST_DATA_DIR)
    fun testBuilding() {
        val defFile = File(TEST_DATA_DIR, DEF_FILE)
        muteCInteropTestIfNecessary(defFile, targets.testTarget)

        val library = cinteropToLibrary(
            targets = targets,
            defFile = defFile,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = File(TEST_DATA_DIR, KT_FILE),
            freeCompilerArgs = TestCompilerArgs(testRunSettings.get<PipelineType>().compilerFlags),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main")
        )
        val compilationResult = compileToExecutable(testCase, library.asLibraryDependency()).assertSuccess()
    }

    companion object {
        const val TEST_DATA_DIR = "native/native.tests/testData/CInterop/KT-63048"
        const val DEF_FILE = "library.def"
        const val KT_FILE = "usage.kt"
    }
}

class CInteropCheckersTest : BaseCInteropCheckersTest()

@FirPipeline
@Tag("frontend-fir")
class FirCInteropCheckersTest : BaseCInteropCheckersTest()
