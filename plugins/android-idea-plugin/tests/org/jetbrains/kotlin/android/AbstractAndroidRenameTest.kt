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
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import kotlin.test.*

public abstract class AbstractAndroidRenameTest : KotlinAndroidTestCase() {

    private val NEW_NAME = "NEWNAME"
    private val OLD_NAME = "OLDNAME"

    public fun doTest(path: String) {
        val f = myFixture!!
        getResourceDirs(path).forEach { myFixture.copyDirectoryToProject(it.name, it.name) }
        val virtualFile = f.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt");
        f.configureFromExistingVirtualFile(virtualFile)

        val editor = f.getEditor()
        val file = f.getFile()
        val completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file)
        val element = TargetElementUtilBase.findTargetElement(
                completionEditor, TargetElementUtilBase.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)

        assert(element != null)
        assertTrue(element is JetProperty)

        RenameProcessor(f.getProject(), element, NEW_NAME, false, true).run()

        // Rename xml attribute by property
        val resolved = GotoDeclarationAction.findTargetElement(f.getProject(), f.getEditor(), f.getCaretOffset())
        assertEquals("\"@+id/$NEW_NAME\"", resolved?.getText())

        // Rename property by attribute
        val attributeElement = GotoDeclarationAction.findTargetElement(f.getProject(), f.getEditor(), f.getCaretOffset())
        RenameProcessor(f.getProject(), attributeElement, "@+id/$OLD_NAME", false, true).run()

        assertEquals(OLD_NAME, (f.getElementAtCaret() as JetProperty).getName())
    }

    override fun getTestDataPath() = KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/rename/" + getTestName(true) + "/"
}
