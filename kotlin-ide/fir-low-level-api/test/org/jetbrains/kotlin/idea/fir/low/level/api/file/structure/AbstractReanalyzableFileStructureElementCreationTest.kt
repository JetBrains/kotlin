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
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractReanalyzableFileStructureElementCreationTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val initialFileText = FileUtil.loadFile(testDataFile)
        val ktFile = myFixture.configureByText(testDataFile.name, initialFileText) as KtFile
        val expectedFqName =
            InTextDirectivesUtils.findStringWithPrefixes(initialFileText, STRUCTURE_ELEMENT_FQ_NAME_DIRECTIVE)
                ?: error("Please specify // STRUCTURE_ELEMENT directive")
        val shouldStructureElementBeRecreated =
            InTextDirectivesUtils.getPrefixedBoolean(initialFileText, SHOULD_ELEMENT_BE_RECREATED)
                ?: error("Please specify // SHOULD_ELEMENT_BE_RECREATED directive")

        val elementAtCaret = ktFile.findElementAtCaret()
        val (initialStructureElement, initialFileStructure, initialModuleResolveState) = getStructureElementForKtElement(elementAtCaret)
        Assert.assertEquals(expectedFqName, initialStructureElement.psi.getKotlinFqName()?.asString())

        myFixture.type("hello")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val newElementAtCaret = ktFile.findElementAtCaret()
        val (newStructureElement, newFileStructure, newModuleResolveState) = getStructureElementForKtElement(newElementAtCaret)
        Assert.assertEquals(
            "Structure elements should be build by the same KtDeclaration's",
            expectedFqName,
            newStructureElement.psi.getKotlinFqName()?.asString()
        )
        Assert.assertTrue("Structure elements should be different after typing", newStructureElement !== initialStructureElement)
        Assert.assertEquals(
            "FirModuleResolveState should change only of out of block modification",
            shouldStructureElementBeRecreated,
            newModuleResolveState !== initialModuleResolveState
        )
        Assert.assertEquals(
            "FileStructure state should change only of out of block modification",
            shouldStructureElementBeRecreated,
            initialFileStructure !== newFileStructure
        )
    }

    private fun KtFile.findElementAtCaret(): KtElement {
        val elementAtCaret = findElementAt(myFixture.caretOffset)!!.parentOfType<KtElement>()!!
        if (elementAtCaret is KtDeclaration) {
            error("Expected element inside declaration but was\n${elementAtCaret.getElementTextInContext()}")
        }
        return elementAtCaret
    }

    private fun getStructureElementForKtElement(element: KtElement): Triple<FileStructureElement, FileStructure, FirModuleResolveState> {
        val moduleResolveState = LowLevelFirApiFacade.getResolveStateFor(element) as FirModuleResolveStateImpl
        val fileStructure =
            moduleResolveState.fileStructureCache.getFileStructure(element.containingKtFile, moduleResolveState.rootModuleSession.cache)
        val fileStructureElement = fileStructure.getStructureElementFor(element)
        return Triple(fileStructureElement, fileStructure, moduleResolveState)
    }

    companion object {
        private const val STRUCTURE_ELEMENT_FQ_NAME_DIRECTIVE = "// STRUCTURE_ELEMENT:"
        private const val SHOULD_ELEMENT_BE_RECREATED = "// SHOULD_ELEMENT_BE_RECREATED:"
    }
}