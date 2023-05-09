/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.ir.linkage.partial.PartialLinkageConfig
import org.jetbrains.kotlin.konan.blackboxtest.AbstractNativeBlackBoxTest
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import java.io.File

/**
 * The directories with original sources (aka testData).
 */
internal class TestRoots(val roots: Set<File>, val baseDir: File)

/**
 * [testSourcesDir] - The directory with generated (preprocessed) test sources.
 * [sharedSourcesDir] - The directory with the sources of the shared modules (i.e. the modules that are widely used in multiple tests).
 */
internal class GeneratedSources(val testSourcesDir: File, val sharedSourcesDir: File)

/**
 * [testBinariesDir] - The directory with compiled test binaries (klibs and executable files).
 * [sharedBinariesDir] - The directory with compiled shared modules (klibs).
 * [givenBinariesDir] - The directory with the given (external) modules (klibs).
 */
internal class Binaries(val testBinariesDir: File, val sharedBinariesDir: File, val givenBinariesDir: File)

/**
 * The [TestConfiguration] of the current test class and the [Annotation] with specific parameters.
 */
internal data class ComputedTestConfiguration(val configuration: TestConfiguration, val annotation: Annotation)

/**
 * The tests (test data files and directories with test data files) that are disabled.
 * This is applicable only to inheritors of [AbstractNativeBlackBoxTest] that use [TestCaseId.TestDataFile].
 */
internal class DisabledTestDataFiles(val filesAndDirectories: Set<File>)

/**
 * Which PL mode and PL log level to use in tests.
 */
internal class UsedPartialLinkageConfig(val config: PartialLinkageConfig)
