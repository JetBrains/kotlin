/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.slicer

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import org.jetbrains.kotlin.idea.test.findFileWithCaret
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractSlicerMultiplatformTest : AbstractMultiModuleTest() {
    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("slicer/mpp")

    protected fun doTest(filePath: String) {
        val testRoot = File(filePath)
        setupMppProjectFromDirStructure(testRoot)

        val file = project.findFileWithCaret() as KtFile
        val document = PsiDocumentManager.getInstance(myProject).getDocument(file)!!
        val offset = document.extractMarkerOffset(project, "<caret>")

        testSliceFromOffset(file, offset) { _, rootNode ->
            KotlinTestUtils.assertEqualsToFile(testRoot.resolve("results.txt"), buildTreeRepresentation(rootNode))
        }
    }
}
