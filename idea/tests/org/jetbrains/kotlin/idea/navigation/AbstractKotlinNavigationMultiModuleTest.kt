/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.GotoSuperActionHandler
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.perf.forceUsingUltraLightClassesForTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import org.jetbrains.kotlin.idea.test.findFileWithCaret
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractKotlinNavigationMultiModuleTest : AbstractMultiModuleTest() {
    protected abstract fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData

    protected fun doTest(testDataDir: String) {
        setupMppProjectFromDirStructure(File(testDataDir))
        val file = project.findFileWithCaret()
        if (InTextDirectivesUtils.isDirectiveDefined(file.text, "ULTRA_LIGHT_CLASSES")) {
            forceUsingUltraLightClassesForTest()
        }

        val doc = PsiDocumentManager.getInstance(myProject).getDocument(file)!!
        val offset = doc.extractMarkerOffset(project, "<caret>")
        val editor = EditorFactory.getInstance().createEditor(doc, myProject)
        editor.caretModel.moveToOffset(offset)
        try {
            val gotoData = doNavigate(editor, file)
            NavigationTestUtils.assertGotoDataMatching(editor, gotoData, true)
        } finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }
}


abstract class AbstractKotlinGotoImplementationMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun getTestDataPath() =
        File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/implementations/multiModule").path + File.separator

    override fun doNavigate(editor: Editor, file: PsiFile) = NavigationTestUtils.invokeGotoImplementations(editor, file)!!
}

abstract class AbstractKotlinGotoSuperMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun getTestDataPath() =
        File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/gotoSuper/multiModule").path + File.separator

    override fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData {
        val (superDeclarations, _) = GotoSuperActionHandler.SuperDeclarationsAndDescriptor.forDeclarationAtCaret(editor, file)
        return GotoTargetHandler.GotoData(file.findElementAt(editor.caretModel.offset)!!, superDeclarations.toTypedArray(), emptyList())
    }
}

abstract class AbstractKotlinGotoRelatedSymbolMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun getTestDataPath() =
        File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/relatedSymbols/multiModule").path + File.separator

    override fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData {
        val source = file.findElementAt(editor.caretModel.offset)!!
        val relatedItems = NavigationUtil.collectRelatedItems(source, null)
        return GotoTargetHandler.GotoData(source, relatedItems.map { it.element }.toTypedArray(), emptyList())
    }
}
