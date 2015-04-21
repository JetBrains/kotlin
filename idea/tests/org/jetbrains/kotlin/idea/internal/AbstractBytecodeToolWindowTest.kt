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

package org.jetbrains.kotlin.idea.internal

import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public abstract class AbstractBytecodeToolWindowTest: JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public fun doTest(testPath: String) {
        val mainDir = File(testPath)
        val mainFileName = mainDir.getName() + ".kt"
        mainDir.listFiles { file, name -> name != mainFileName }.forEach { myFixture.configureByFile(testPath + "/" + it.getName()) }

        val mainFileText = File("$testPath/$mainFileName").readText()
        myFixture.configureByText(JetFileType.INSTANCE, mainFileText)

        val file = myFixture.getFile() as JetFile

        val enableInline = InTextDirectivesUtils.getPrefixedBoolean(mainFileText, "// INLINE:") ?: true
        val bytecodes = KotlinBytecodeToolWindow.getBytecodeForFile(file, enableInline, true, true)
        assert(bytecodes.contains("// ================")) {
            "The header \"// ================\" is missing.\n This means that there is an exception failed during compilation:\n$bytecodes"
        }
    }
}

