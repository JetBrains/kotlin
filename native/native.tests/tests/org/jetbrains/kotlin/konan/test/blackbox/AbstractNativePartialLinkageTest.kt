/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCScheduler
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("partial-linkage")
@UsePartialLinkage(UsePartialLinkage.Mode.DEFAULT)
abstract class AbstractNativePartialLinkageTest : AbstractKlibLinkageTest() {

    // The entry point to generated test classes.
    protected fun runTest(@TestDataFile testPath: String) {
        // KT-70162: Partial Linkage tests take a lot of time when aggressive scheduler is enabled.
        // There is no major profit from running these tests with this scheduler. On the other hand,
        // we have to significantly increase timeouts to make such configurations pass.
        // So let's just disable them instead of wasting CI times.
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>() == GCScheduler.AGGRESSIVE)

        KlibCompilerInvocationTestUtils.runTest(
            testConfiguration = NativeTestConfiguration(testPath),
            compilerEditionChange = KlibCompilerChangeScenario.NoChange,
        )
    }

}
