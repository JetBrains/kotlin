package org.jetbrains.kotlin.konan.test.support

import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.NativeTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ThreadStateChecker
import org.jetbrains.kotlin.konan.test.syntheticAccessors.AbstractNativeKlibSyntheticAccessorTest
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.KlibBasedCompilerTestDirectives
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Used to run tests for IR inlining and synthetic accessors. This test helper effectively does the following:
 * - Enables IR visibility validation.
 * - Disables LLVM-related phases, so the compilation effectively ends at the last IR lowering.
 * - Ensures double inlining mode is always turned on.
 *
 * TODO(KT-64570): Migrate these tests to the Core test infrastructure as soon as we move IR inlining to the 1st compilation stage.
 */
class KlibSyntheticAccessorTestSupport : BeforeEachCallback {
    override fun beforeEach(extensionContext: ExtensionContext): Unit = with(extensionContext) {
        val nativeTestInstances = computeKlibSyntheticAccessorTestInstances()
        val settings = createTestRunSettings(nativeTestInstances) {
            with(RegisteredDirectivesBuilder()) {
                +KlibBasedCompilerTestDirectives.DUMP_KLIB_SYNTHETIC_ACCESSORS

                TestDirectives.FREE_COMPILER_ARGS with listOfNotNull(
                    // Don't run LLVM, stop after the last IR lowering.
                    "-Xdisable-phases=LinkBitcodeDependencies,WriteBitcodeFile,ObjectFiles,Linker",

                    // Enable double-inlining.
                    "-Xklib-no-double-inlining=false",

                    // Enable narrowing of visibility for synthetic accessors.
                    "-Xsynthetic-accessors-with-narrowed-visibility".takeIf { nativeTestInstances.enclosingTestInstance.narrowedAccessorVisibility }
                )

                build()
            }
        }

        assumeTrue(settings.get<CacheMode>() == CacheMode.WithoutCache)
        assumeTrue(settings.get<ThreadStateChecker>() == ThreadStateChecker.DISABLED)

        // Inject the required properties to test instance.
        with(nativeTestInstances.enclosingTestInstance) {
            testRunSettings = settings
            testRunProvider = getOrCreateTestRunProvider()
        }
    }
}

internal fun ExtensionContext.computeKlibSyntheticAccessorTestInstances(): NativeTestInstances<AbstractNativeKlibSyntheticAccessorTest> =
    NativeTestInstances(requiredTestInstances.allInstances)

