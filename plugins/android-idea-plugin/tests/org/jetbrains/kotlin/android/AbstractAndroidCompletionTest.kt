/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.completion.test.testCompletion
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.io.File

abstract class AbstractAndroidCompletionTest : KotlinAndroidTestCase() {
    private var codeCompletionOldValue: Boolean = false
    private var smartTypeCompletionOldValue: Boolean = false

    override fun setUp() {
        super.setUp()

        val settings = CodeInsightSettings.getInstance()
        codeCompletionOldValue = settings.AUTOCOMPLETE_ON_CODE_COMPLETION
        smartTypeCompletionOldValue = settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (completionType()) {
            CompletionType.SMART -> settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = false
            CompletionType.BASIC -> settings.AUTOCOMPLETE_ON_CODE_COMPLETION = false
        }
    }

    private fun completionType() = CompletionType.BASIC

    fun doTest(path: String?) {
        getResourceDirs(path).forEach { myFixture.copyDirectoryToProject(it.name, it.name) }
        val virtualFile = myFixture.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt");
        myFixture.configureFromExistingVirtualFile(virtualFile)
        val fileText = FileUtil.loadFile(File(path + getTestName(true) + ".kt"), true)
        testCompletion(fileText, JvmPlatform, { completionType, count -> myFixture.complete(completionType, count) })
    }


    override fun getTestDataPath(): String {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/completion/" + getTestName(true) + "/"
    }

    override fun tearDown() {
        val settings = CodeInsightSettings.getInstance()
        settings.AUTOCOMPLETE_ON_CODE_COMPLETION = codeCompletionOldValue
        settings.AUTOCOMPLETE_ON_SMART_TYPE_COMPLETION = smartTypeCompletionOldValue

        super.tearDown()
    }
}
