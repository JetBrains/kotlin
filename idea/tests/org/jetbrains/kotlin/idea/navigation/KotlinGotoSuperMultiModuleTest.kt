/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.GotoSuperActionHandler
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class KotlinGotoSuperMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData {
        val (superDeclarations, _) = GotoSuperActionHandler.SuperDeclarationsAndDescriptor.forDeclarationAtCaret(editor, file)
        return GotoTargetHandler.GotoData(file.findElementAt(editor.caretModel.offset)!!, superDeclarations.toTypedArray(), emptyList())
    }

    override fun getTestDataPath() =
        File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/gotoSuper/multiModule").path + File.separator

    fun testActualClass() {
        doMultiPlatformTest("jvm.kt")
    }

    fun testActualFunction() {
        doMultiPlatformTest("jvm.kt")
    }

    fun testActualProperty() {
        doMultiPlatformTest("jvm.kt")
    }
}