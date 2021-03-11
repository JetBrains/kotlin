/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.AbstractProjectWideOutOfBlockKotlinModificationTrackerTest
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractFileStructureAndOutOfBlockModificationTrackerConsistencyTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val fileText = FileUtil.loadFile(testDataFile)
        val ktFile = myFixture.configureByText(testDataFile.name, fileText) as KtFile
        val textToType = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// TYPE:")
            ?: AbstractProjectWideOutOfBlockKotlinModificationTrackerTest.DEFAULT_TEXT_TO_TYPE
        val outOfBlock = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// OUT_OF_BLOCK:")
            ?: error("Please, specify should out of block change happen or not by `// OUT_OF_BLOCK:` directive")

        val elementAtCaret = ktFile.findElementAtCaret()

        var fileStructureWithResolveState = getStructureElementForKtElement(elementAtCaret)
        for (i in 0..REPETITIONS_COUNT) {
            fileStructureWithResolveState = typeAndCheck(ktFile, fileStructureWithResolveState, textToType, outOfBlock)
        }
    }

    private fun typeAndCheck(
        ktFile: KtFile,
        fileStructureWithResolveStateBeforeTyping: FileStructureWithResolveState,
        textToType: String,
        outOfBlock: Boolean,
    ): FileStructureWithResolveState {
        val newFileStructureWithResolveState = getStateAfterTyping(textToType, ktFile)
        checkIsCorrect(newFileStructureWithResolveState, fileStructureWithResolveStateBeforeTyping, outOfBlock)
        return newFileStructureWithResolveState
    }

    private fun getStateAfterTyping(
        textToType: String,
        ktFile: KtFile
    ): FileStructureWithResolveState {
        myFixture.type(textToType)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val newElementAtCaret = ktFile.findElementAtCaret()
        return getStructureElementForKtElement(newElementAtCaret)
    }

    private fun checkIsCorrect(
        newFileStructureWithResolveState: FileStructureWithResolveState,
        fileStructureWithResolveStateBeforeTyping: FileStructureWithResolveState,
        outOfBlock: Boolean
    ) {
        assertTrue(
            "Structure elements should be different after typing",
            newFileStructureWithResolveState.element !== fileStructureWithResolveStateBeforeTyping.element
        )

        assertEquals(
            "FirModuleResolveState should change only on out of block modification",
            outOfBlock,
            newFileStructureWithResolveState.resolveState !== fileStructureWithResolveStateBeforeTyping.resolveState
        )
        assertEquals(
            "FileStructure state should change only on out of block modification",
            outOfBlock,
            newFileStructureWithResolveState.fileStructure !== fileStructureWithResolveStateBeforeTyping.fileStructure
        )
    }

    private fun KtFile.findElementAtCaret(): KtElement {
        val element = when (val elementAtOffset = findElementAt(myFixture.caretOffset)) {
            is PsiWhiteSpace -> findElementAt((myFixture.caretOffset - 1).coerceAtLeast(0))
            else -> elementAtOffset
        }
        return element!!.parentOfType()!!
    }

    private fun getStructureElementForKtElement(element: KtElement): FileStructureWithResolveState {
        val moduleResolveState = element.getResolveState() as FirModuleResolveStateImpl
        val fileStructure =
            moduleResolveState.fileStructureCache.getFileStructure(element.containingKtFile, moduleResolveState.rootModuleSession.cache)
        val fileStructureElement = fileStructure.getStructureElementFor(element)
        return FileStructureWithResolveState(fileStructureElement, fileStructure, moduleResolveState)
    }

    private data class FileStructureWithResolveState(
        val element: FileStructureElement,
        val fileStructure: FileStructure,
        val resolveState: FirModuleResolveState
    )

    companion object {
        private const val REPETITIONS_COUNT = 3
    }
}