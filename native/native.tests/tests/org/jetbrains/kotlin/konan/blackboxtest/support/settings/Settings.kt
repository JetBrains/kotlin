/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.settings

import org.jetbrains.kotlin.konan.blackboxtest.support.util.TestDisposable
import java.io.File

internal class Settings(
    val global: GlobalSettings,
    val testRoots: TestRoots, // The directories with original sources (aka testData).
    val testSourcesDir: File, // The directory with generated (preprocessed) test sources.
    val sharedSourcesDir: File, // The directory with the sources of the shared modules (i.e. the modules that are widely used in multiple tests).
    val testBinariesDir: File, // The directory with compiled test binaries (klibs) and executable files).
    val sharedBinariesDir: File // The directory with compiled shared modules (klibs).
) : TestDisposable(parentDisposable = null)

internal class TestRoots(
    val roots: Set<File>,
    val baseDir: File
)
