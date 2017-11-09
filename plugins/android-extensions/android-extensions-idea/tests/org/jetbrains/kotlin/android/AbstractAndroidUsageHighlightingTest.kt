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

package org.jetbrains.kotlin.android

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.highlighting.actions.HighlightUsagesAction
import com.intellij.testFramework.ExpectedHighlightingData


abstract class AbstractAndroidUsageHighlightingTest : KotlinAndroidTestCase() {
    fun doTest(path: String) {
        copyResourceDirectoryForTest(path)
        val virtualFile = myFixture.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        val document = myFixture.editor.document
        val data = ExpectedHighlightingData(
                document,
                false,
                false,
                true,
                false,
                myFixture.file)

        data.init()

        myFixture.testAction(HighlightUsagesAction())

        val infos = myFixture.editor.markupModel.allHighlighters.map {
            HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(it.startOffset, it.endOffset).create()
        }

        data.checkResult(infos, document.text)
    }
}