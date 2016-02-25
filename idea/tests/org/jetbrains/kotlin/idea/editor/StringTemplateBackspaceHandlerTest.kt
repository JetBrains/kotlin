/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.editor

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.EditorTestUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

class StringTemplateBackspaceHandlerTest : AbstractEditorTest() {
    val path = File(PluginTestCaseBase.getTestDataPathBase(), "/editor/stringTemplateBackspaceHandler/").getAbsolutePath()

    override fun getBasePath(): String? {
        return path
    }

    fun testStringTemplateBrackets() {
        doTest()
    }

    fun testEscapedStringTemplate() {
        doTest()
    }

    fun doTest() {
        configure()
        EditorTestUtil.executeAction(getEditor(), IdeActions.ACTION_EDITOR_BACKSPACE)
        check()
    }
}
