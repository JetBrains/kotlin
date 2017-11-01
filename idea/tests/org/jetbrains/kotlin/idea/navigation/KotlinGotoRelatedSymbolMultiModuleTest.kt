/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class KotlinGotoRelatedSymbolMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun getTestDataPath() =
            File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/relatedSymbols/multiModule").path + File.separator

    override fun doNavigate(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData {
        val source = file.findElementAt(editor.caretModel.offset)!!
        val relatedItems = NavigationUtil.collectRelatedItems(source, null)
        return GotoTargetHandler.GotoData(source, relatedItems.map { it.element }.toTypedArray(), emptyList())
    }

    fun testFromTopLevelExpectClassToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromTopLevelActualClassToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }

    fun testFromTopLevelExpectFunToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromTopLevelActualFunToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }

    fun testFromTopLevelExpectValToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromTopLevelActualValToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }

    fun testFromNestedExpectClassToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromNestedActualClassToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }

    fun testFromExpectMemberFunToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromActualMemberFunToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }

    fun testFromExpectMemberValToActuals() {
        doMultiPlatformTestJvmJs("common.kt")
    }

    fun testFromActualMemberValToExpect() {
        doMultiPlatformTestJvmJs("jvm.kt")
    }
}