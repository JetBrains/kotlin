/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.collectDescendantsOfType
import com.intellij.psi.util.forEachDescendantOfType
import com.intellij.psi.util.parentOfType
import junit.framework.Assert
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getResolveState
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.isFakeElement
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractFileStructureTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val initialFileText = FileUtil.loadFile(testDataFile)
        val ktFile = myFixture.configureByText(testDataFile.name, initialFileText) as KtFile
        val fileStructure = ktFile.getFileStructure()
        val allStructureElements = fileStructure.getAllStructureElements(ktFile)
        val declarationToStructureElement = allStructureElements.associateBy { it.psi }
        runUndoTransparentWriteAction {
            ktFile.collectDescendantsOfType<PsiComment>().forEach { it.delete() }
            ktFile.forEachDescendantOfType<KtDeclaration> { ktDeclaration ->
                val structureElement = declarationToStructureElement[ktDeclaration] ?: return@forEachDescendantOfType
                val comment = structureElement.createComment()
                 when (ktDeclaration) {
                     is KtClassOrObject -> {
                         val lBrace = ktDeclaration.body?.lBrace
                         if (lBrace != null) {
                             ktDeclaration.body!!.addAfter(comment, lBrace)
                         } else {
                             ktDeclaration.parent.addAfter(comment, ktDeclaration)
                         }
                     }
                     is KtFunction -> {
                         val lBrace = ktDeclaration.bodyBlockExpression?.lBrace
                         if (lBrace != null) {
                             ktDeclaration.bodyBlockExpression!!.addAfter(comment, lBrace)
                         } else {
                             ktDeclaration.parent.addAfter(comment, ktDeclaration)
                         }
                     }
                    else -> error("Unsupported declaration $ktDeclaration")
                }
            }
        }
        KotlinTestUtils.assertEqualsToFile(testDataFile, ktFile.text)
    }

    private fun FileStructureElement.createComment(): PsiComment {
        val text = """/* ${this::class.simpleName!!} */"""
        return KtPsiFactory(psi.project).createComment(text)
    }

    private fun KtFile.getFileStructure(): FileStructure {
        val moduleResolveState = getResolveState() as FirModuleResolveStateImpl
        return moduleResolveState.fileStructureCache.getFileStructure(
            ktFile = this,
            moduleFileCache = moduleResolveState.rootModuleSession.cache
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FileStructure.getAllStructureElements(ktFile: KtFile): Collection<FileStructureElement> = buildSet {
        ktFile.forEachDescendantOfType<KtElement> { ktElement ->
            add(getStructureElementFor(ktElement))
        }
    }

}