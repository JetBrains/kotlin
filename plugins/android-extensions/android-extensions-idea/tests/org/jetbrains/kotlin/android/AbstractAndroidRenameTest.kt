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

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

abstract class AbstractAndroidRenameTest : KotlinAndroidTestCase() {
    private val NEW_NAME = "NEWNAME"
    private val NEW_ID_NAME = "@+id/$NEW_NAME"

    fun doTest(path: String) {
        getResourceDirs(path).forEach { myFixture.copyDirectoryToProject(it.name, it.name) }
        val virtualFile = myFixture.copyFileToProject("$path${getTestName(true)}.kt", "src/${getTestName(true)}.kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)
        myFixture.renameElement(myFixture.elementAtCaret, NEW_ID_NAME)
        myFixture.checkResultByFile("expected/${getTestName(true)}.kt")
        assertResourcesEqual("$path/expected/res")
    }

    fun assertResourcesEqual(expectedPath: String) {
        PlatformTestUtil.assertDirectoriesEqual(LocalFileSystem.getInstance().findFileByPath(expectedPath), getResourceDirectory())
    }

    fun getResourceDirectory() = LocalFileSystem.getInstance().findFileByPath(myFixture.tempDirPath + "/res")

    override fun getTestDataPath() = KotlinAndroidTestCaseBase.getPluginTestDataPathBase() + "/rename/" + getTestName(true) + "/"
}
