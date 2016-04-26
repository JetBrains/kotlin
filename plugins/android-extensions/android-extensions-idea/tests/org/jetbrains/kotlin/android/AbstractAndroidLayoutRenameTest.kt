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

package org.jetbrains.kotlin.android

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractAndroidLayoutRenameTest : KotlinAndroidTestCase() {
    private val NEW_NAME = "new_name"
    private val NEW_NAME_XML = "$NEW_NAME.xml"

    fun doTest(path: String) {
        val f = myFixture!!
        getResourceDirs(path).forEach { myFixture.copyDirectoryToProject(it.name, it.name) }
        val virtualFile = f.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt")
        f.configureFromExistingVirtualFile(virtualFile)

        val completionEditor = InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(f.editor, f.file)

        val element = TargetElementUtil.findTargetElement(
                completionEditor,
                TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.ELEMENT_NAME_ACCEPTED) as XmlAttributeValueImpl

        val file = element.containingFile as XmlFile

        RenameProcessor(f.project, file, NEW_NAME_XML, false, true).run()

        (f.file as KtFile).importDirectives.any { it.importedFqName!!.asString() == AndroidConst.SYNTHETIC_PACKAGE + ".main." + NEW_NAME }
    }

    override fun getTestDataPath() = KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/rename/" + getTestName(true) + "/"
}
