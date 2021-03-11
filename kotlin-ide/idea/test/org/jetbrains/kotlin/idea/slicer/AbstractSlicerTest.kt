/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.openapi.util.io.FileUtil
import com.intellij.slicer.SliceRootNode
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractSlicerTest : KotlinLightCodeInsightFixtureTestCase() {
    protected abstract fun doTest(path: String, sliceProvider: KotlinSliceProvider, rootNode: SliceRootNode)

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val rootDir = mainFile.parentFile

        val namePrefix = FileUtil.getNameWithoutExtension(mainFile)
        val extraFiles = rootDir.listFiles { _, name ->
            name.startsWith("$namePrefix.") && PathUtil.getFileExtension(name).let { it == "kt" || it == "java" }
        }!!

        myFixture.testDataPath = rootDir.path

        val extraPsiFiles = extraFiles.map { myFixture.configureByFile(it.name) }
        val file = myFixture.configureByFile(mainFile.name) as KtFile

        // check correctness of test data
        extraPsiFiles.forEach {
            myFixture.testHighlighting(false, false, false, it.virtualFile)
        }
        myFixture.testHighlighting(false, false, false, file.virtualFile)

        testSliceFromOffset(file, editor.caretModel.offset) { sliceProvider, rootNode ->
            doTest(path, sliceProvider, rootNode)
        }
    }
}