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

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class KotlinGotoImplementationMultiModuleTest : AbstractKotlinNavigationMultiModuleTest() {
    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/navigation/implementations/multiModule").path + File.separator
    }

    override fun doNavigate(editor: Editor, file: PsiFile) = NavigationTestUtils.invokeGotoImplementations(editor, file)

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