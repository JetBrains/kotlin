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

import com.intellij.openapi.editor.EditorFactory
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.stubs.createFacet
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.extractMarkerOffset
import java.io.File

class KotlinGotoImplementationMultiModuleTest : AbstractMultiModuleTest() {
    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/implementations/multiModule").path + File.separator
    }

    private fun doMultiPlatformTest(
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

        val file = PluginJetFilesProvider.allFilesInProject(myProject).single { it.name == testFileName }
        val doc = PsiDocumentManager.getInstance(myProject).getDocument(file)!!
        val offset = doc.extractMarkerOffset(project, "<caret>")
        val editor = EditorFactory.getInstance().createEditor(doc, myProject)
        editor.caretModel.moveToOffset(offset)
        try {
            val gotoData = NavigationTestUtils.invokeGotoImplementations(editor, file)
            NavigationTestUtils.assertGotoDataMatching(editor, gotoData, true)
        }
        finally {
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    fun testSuspendFunImpl() {
        doMultiPlatformTest(
                "common.kt",
                actuals = *arrayOf("jvm" to TargetPlatformKind.Jvm[JvmTarget.JVM_1_6], "js" to TargetPlatformKind.JavaScript)
        )
    }

    fun testExpectClassSuperclass() {
        doMultiPlatformTest("common.kt")
    }

    fun testExpectClassSuperclassFun() {
        doMultiPlatformTest("common.kt")
    }

    fun testExpectClassSuperclassProperty() {
        doMultiPlatformTest("common.kt")
    }

    fun testExpectClass() {
        doMultiPlatformTest("common.kt")
    }

    fun testExpectClassFun() {
        doMultiPlatformTest("common.kt")
    }

    fun testExpectClassProperty() {
        doMultiPlatformTest("common.kt")
    }
}