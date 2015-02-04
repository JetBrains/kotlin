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

import org.jetbrains.kotlin.psi.JetProperty
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction

public abstract class AbstractAndroidGotoTest : KotlinAndroidTestCase() {

    override fun getTestDataPath(): String {
        return KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/goto/" + getTestName(true) + "/"
    }

    public fun doTest(path: String) {
        val f = myFixture!!
        f.copyDirectoryToProject(getResDir()!!, "res")
        val virtualFile = f.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt");
        f.configureFromExistingVirtualFile(virtualFile)

        val resolved = GotoDeclarationAction.findTargetElement(f.getProject(), f.getEditor(), f.getCaretOffset())
        if (f.getElementAtCaret() !is JetProperty) kotlin.test.fail("element at caret must be a property, not a ${f.getElementAtCaret().javaClass}")
        kotlin.test.assertEquals("\"@+id/${(f.getElementAtCaret() as JetProperty).getName()}\"", resolved?.getText())

    }
}