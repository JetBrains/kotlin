/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KotlinpCompilerTestDataTest(private val file: File) {
    private class TestDisposable : Disposable {
        override fun dispose() {}
    }

    @Test
    fun doTest() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this::class.java.simpleName, file.nameWithoutExtension)

        val disposable = TestDisposable()
        try {
            compileAndPrintAllFiles(file, disposable, tmpdir, compareWithTxt = false, readWriteAndCompare = true)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun computeTestDataFiles(): Collection<Array<*>> {
            val baseDirs = listOf(
                "compiler/testData/loadJava/compiledKotlin",
                "compiler/testData/serialization/builtinsSerializer"
            )

            return mutableListOf<Array<*>>().apply {
                for (baseDir in baseDirs) {
                    for (file in File(baseDir).walkTopDown()) {
                        if (file.extension == "kt") {
                            add(arrayOf(file))
                        }
                    }
                }
            }
        }
    }
}
