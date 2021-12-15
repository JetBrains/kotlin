/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

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
 */
internal class Binaries(val testBinariesDir: File, val sharedBinariesDir: File)

/**
 * The [TestConfiguration] of the current test class and the [Annotation] with specific parameters.
 */
internal data class ComputedTestConfiguration(val configuration: TestConfiguration, val annotation: Annotation)
