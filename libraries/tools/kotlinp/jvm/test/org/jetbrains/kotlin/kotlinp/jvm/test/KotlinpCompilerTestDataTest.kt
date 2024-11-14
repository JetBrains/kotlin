/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KotlinpCompilerTestDataTest(private val file: File) {
    private class TestDisposable : Disposable {
        override fun dispose() {}
    }

    private fun doTest(useK2: Boolean) {
        val tmpdir = KtTestUtil.tmpDirForTest(this::class.java.simpleName, file.nameWithoutExtension)

        val disposable = TestDisposable()
        try {
            compareAllFiles(file, disposable, tmpdir, compareWithTxt = false, readWriteAndCompare = true, useK2 = useK2)
        } finally {
            disposeRootInWriteAction(disposable)
        }
    }

    @Test
    fun doTestK1() {
        doTest(useK2 = false)
    }

    @Test
    fun doTestK2() {
        doTest(useK2 = true)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun computeTestDataFiles(): Collection<Array<*>> {
            val baseDirs = listOf(
                "compiler/testData/loadJava/compiledKotlin",
                "compiler/testData/loadJava/compiledKotlinWithStdlib",
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
