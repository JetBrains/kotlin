/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.After
import java.io.File
import kotlin.test.fail

abstract class AbstractObjCExportHeaderGeneratorTest(
    private val generator: ObjCExportHeaderGenerator
) {

    fun interface ObjCExportHeaderGenerator {
        fun generateHeaders(disposable: Disposable, root: File): String
    }

    private val testRootDisposable = Disposer.newDisposable("${AbstractObjCExportHeaderGeneratorTest::class.simpleName}.testRootDisposable")
    protected val objCExportTestDataDir = testDataDir.resolve("objcexport")

    @After
    fun dispose() {
        Disposer.dispose(testRootDisposable)
    }


    protected fun doTest(root: File) {
        if (!root.isDirectory) fail("Expected ${root.absolutePath} to be directory")
        val generatedHeaders = generator.generateHeaders(testRootDisposable, root)
        KotlinTestUtils.assertEqualsToFile(root.resolve("!${root.nameWithoutExtension}.h"), generatedHeaders)
    }
}

abstract class AbstractFE10ObjCExportHeaderGeneratorTest : AbstractObjCExportHeaderGeneratorTest(Fe10ObjCExportHeaderGenerator)
