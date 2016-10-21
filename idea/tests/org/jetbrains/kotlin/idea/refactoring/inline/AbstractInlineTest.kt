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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtil.ELEMENT_NAME_ACCEPTED
import com.intellij.codeInsight.TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractInlineTest : KotlinLightCodeInsightFixtureTestCase() {
    val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val afterFile = File(path + ".after")

        val mainFileName = mainFile.name
        val mainFileBaseName = FileUtil.getNameWithoutExtension(mainFileName)
        val extraFiles = mainFile.parentFile.listFiles { file, name ->
            name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java"))
        }
        val extraFilesToPsi = extraFiles.associateBy { fixture.configureByFile(path.replace(mainFileName, it.name)) }
        val file = myFixture.configureByFile(path)

        val afterFileExists = afterFile.exists()

        val targetElement = TargetElementUtil.findTargetElement(myFixture.editor, ELEMENT_NAME_ACCEPTED or REFERENCED_ELEMENT_ACCEPTED)!!
        val handler = Extensions.getExtensions(InlineActionHandler.EP_NAME).firstOrNull { it.canInlineElement(targetElement) }
        val expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.file.text, "// ERROR: ")
        if (handler != null) {
            try {
                runWriteAction { handler.inlineElement(myFixture.project, myFixture.editor, targetElement) }

                UsefulTestCase.assertEmpty(expectedErrors)
                KotlinTestUtils.assertEqualsToFile(afterFile, file.text)
                for ((extraPsiFile, extraFile) in extraFilesToPsi) {
                    KotlinTestUtils.assertEqualsToFile(File("${extraFile.path}.after"), extraPsiFile.text)
                }
            }
            catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
                TestCase.assertFalse(afterFileExists)
                TestCase.assertEquals(1, expectedErrors.size)
                TestCase.assertEquals(expectedErrors[0].replace("\\n", "\n"), e.message)
            }
        }
        else {
            TestCase.assertFalse(afterFileExists)
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
