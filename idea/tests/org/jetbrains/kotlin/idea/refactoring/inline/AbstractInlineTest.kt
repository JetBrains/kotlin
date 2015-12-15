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
import com.intellij.openapi.util.io.FileUtil
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractInlineTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        val afterFile = File(path + ".after")

        myFixture.configureByFile(path)

        val afterFileExists = afterFile.exists()

        val targetElement = TargetElementUtil.findTargetElement(myFixture.editor, ELEMENT_NAME_ACCEPTED or REFERENCED_ELEMENT_ACCEPTED)!!
        val handler = KotlinInlineValHandler()

        val expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.file.text, "// ERROR: ")
        if (handler.canInlineElement(targetElement)) {
            try {
                runWriteAction { handler.inlineElement(myFixture.project, myFixture.editor, targetElement) }

                TestCase.assertTrue(afterFileExists)
                UsefulTestCase.assertEmpty(expectedErrors)
                myFixture.checkResult(FileUtil.loadFile(afterFile, true))
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
