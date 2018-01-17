/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.idea.test.extractMarkerOffset

abstract class AbstractKotlinNavigationMultiModuleTest : AbstractMultiModuleTest() {
    protected abstract fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData

    protected fun doMultiPlatformTest(
            testFileName: String,
            commonModuleName: String = "common",
            vararg actuals: Pair<String, TargetPlatformKind<*>> = arrayOf("jvm" to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6])
    ) {
        val commonModule = module(commonModuleName)
        commonModule.createFacet(TargetPlatformKind.Common, false)

        actuals.forEach { (actualName, actualKind) ->
            val implModule = module(actualName)
            implModule.createFacet(actualKind, implementedModuleName = commonModuleName)
            implModule.enableMultiPlatform()
            implModule.addDependency(commonModule)
        }

        val file = project.allKotlinFiles().single { it.name == testFileName }
        val doc = PsiDocumentManager.getInstance(myProject).getDocument(file)!!
        val offset = doc.extractMarkerOffset(project, "<caret>")
        val editor = EditorFactory.getInstance().createEditor(doc, myProject)
        editor.caretModel.moveToOffset(offset)
        try {
            val gotoData = doNavigate(editor, file)
            NavigationTestUtils.assertGotoDataMatching(editor, gotoData, true)
        }
        finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    protected fun doMultiPlatformTestJvmJs(testFileName: String, commonModuleName: String = "common") {
        doMultiPlatformTest(
                testFileName,
                commonModuleName,
                *arrayOf("jvm" to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], "js" to TargetPlatformKind.JavaScript)
        )
    }
}