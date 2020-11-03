/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.parentOfType
import junit.framework.Assert
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
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
        val (initialStructureElement, initialFileStructure, initialModuleResolveState) = getStructureElementForKtElement(elementAtCaret)

        myFixture.type(textToType)
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val newElementAtCaret = ktFile.findElementAtCaret()
        val (newStructureElement, newFileStructure, newModuleResolveState) = getStructureElementForKtElement(newElementAtCaret)
        Assert.assertTrue("Structure elements should be different after typing", newStructureElement !== initialStructureElement)

        Assert.assertEquals(
            "FirModuleResolveState should change only on out of block modification",
            outOfBlock,
            newModuleResolveState !== initialModuleResolveState
        )
        Assert.assertEquals(
            "FileStructure state should change only on out of block modification",
            outOfBlock,
            initialFileStructure !== newFileStructure
        )
    }

    private fun KtFile.findElementAtCaret(): KtElement =
        findElementAt(myFixture.caretOffset)!!.parentOfType()!!

    private fun getStructureElementForKtElement(element: KtElement): Triple<FileStructureElement, FileStructure, FirModuleResolveState> {
        val moduleResolveState = LowLevelFirApiFacade.getResolveStateFor(element) as FirModuleResolveStateImpl
        val fileStructure =
            moduleResolveState.fileStructureCache.getFileStructure(element.containingKtFile, moduleResolveState.rootModuleSession.cache)
        val fileStructureElement = fileStructure.getStructureElementFor(element)
        return Triple(fileStructureElement, fileStructure, moduleResolveState)
    }
}