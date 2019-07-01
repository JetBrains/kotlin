/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

abstract class AbstractKotlinpTest : TestCaseWithTmpdir() {
    protected fun doTest(fileName: String) {
        compileAndPrintAllFiles(File(fileName), testRootDisposable, tmpdir, compareWithTxt = true, readWriteAndCompare = true)
    }
}
