/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.android

import org.jetbrains.jet.plugin.PluginTestCaseBase
import com.intellij.openapi.application.PathManager
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.completion.util.testCompletion
import org.jetbrains.jet.plugin.project.TargetPlatform
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.plugin.actions.internal.KotlinInternalMode
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.PsiManager
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver
import com.android.SdkConstants
import com.intellij.codeInsight.CodeInsightSettings

public abstract class AbstractAndroidCompletionTest : KotlinAndroidTestCase() {
    private var kotlinInternalModeOriginalValue: Boolean = false

    private var codeCompletionOldValue: Boolean = false
    private var smartTypeCompletionOldValue: Boolean = false

    override fun setUp() {
        super.setUp()

        val settings = CodeInsightSettings.getInstance()
        codeCompletionOldValue = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
        smartTypeCompletionOldValue = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION

        when (completionType()) {
            CompletionType.SMART -> settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = false
            CompletionType.BASIC -> settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false
        }
    }

    private fun completionType() = CompletionType.BASIC

    fun doTest(testPath: String?) {
        myFixture.copyDirectoryToProject(getResDir()!!, "res")
        val virtualFile = myFixture.copyFileToProject(testPath + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt");
        myFixture.configureFromExistingVirtualFile(virtualFile)
        val fileText = FileUtil.loadFile(File(testPath + getTestName(true) + ".kt"), true)
        testCompletion(fileText, TargetPlatform.JVM, {
            count -> myFixture.complete(completionType())
        })
    }


    override fun getTestDataPath(): String {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/completion/" + getTestName(true) + "/"
    }

    override fun tearDown() {
        val settings = CodeInsightSettings.getInstance()
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = codeCompletionOldValue
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = smartTypeCompletionOldValue

        super<KotlinAndroidTestCase>.tearDown()
    }
}
