/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

abstract class AbstractKotlinpTest : TestCaseWithTmpdir() {
    abstract fun useK2(): Boolean

    protected fun doTest(fileName: String) {
        compareAllFiles(
            File(fileName),
            testRootDisposable,
            tmpdir,
            compareWithTxt = true,
            readWriteAndCompare = true,
            useK2 = useK2()
        )
    }
}
