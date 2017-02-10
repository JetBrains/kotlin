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

package org.jetbrains.kotlin.idea.internal

import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractBytecodeToolWindowTest: KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun doTest(testPath: String) {
        val mainDir = File(testPath)
        val mainFileName = mainDir.name + ".kt"
        mainDir.listFiles { _, name -> name != mainFileName }.forEach { myFixture.configureByFile(testPath + "/" + it.name) }

        val mainFileText = File("$testPath/$mainFileName").readText()
        myFixture.configureByText(KotlinFileType.INSTANCE, mainFileText)

        val file = myFixture.file as KtFile

        val configuration = CompilerConfiguration().apply {
            if (InTextDirectivesUtils.getPrefixedBoolean(mainFileText, "// INLINE:") == false) {
                put(CommonConfigurationKeys.DISABLE_INLINE, true)
            }

            languageVersionSettings = file.languageVersionSettings
        }

        val bytecodes = KotlinBytecodeToolWindow.getBytecodeForFile(file, configuration)
        assert(bytecodes.contains("// ================")) {
            "The header \"// ================\" is missing.\n This means that there is an exception failed during compilation:\n$bytecodes"
        }
    }
}
