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

import com.intellij.psi.xml.XmlFile
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractAndroidLayoutRenameTest : KotlinAndroidTestCase() {
    private val NEW_NAME = "new_name"
    private val NEW_NAME_XML = "$NEW_NAME.xml"

    fun doTest(path: String) {
        copyResourceDirectoryForTest(path)
        val virtualFile = myFixture.copyFileToProject(path + getTestName(true) + ".kt", "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)
        val xmlFile = myFixture.elementAtCaret.containingFile as XmlFile

        myFixture.renameElement(xmlFile, NEW_NAME_XML)

        val expectedImportName = AndroidConst.SYNTHETIC_PACKAGE + ".main." + NEW_NAME
        assertTrue((myFixture.file as KtFile).importDirectives.any { it.importedFqName?.asString() == expectedImportName })
    }
}
