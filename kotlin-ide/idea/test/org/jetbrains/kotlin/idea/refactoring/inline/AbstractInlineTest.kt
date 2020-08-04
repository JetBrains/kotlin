/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtil.ELEMENT_NAME_ACCEPTED
import com.intellij.codeInsight.TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.util.io.FileUtil
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractInlineTest : KotlinLightCodeInsightFixtureTestCase() {
    val fixture: JavaCodeInsightTestFixture
        get() = myFixture

    protected fun doTest(unused: String) {
        val testDataFile = testDataFile()
        val afterFile = File(testDataPath, "${fileName()}.after")

        val mainFileName = testDataFile.name
        val mainFileBaseName = FileUtil.getNameWithoutExtension(mainFileName)
        val extraFiles = testDataFile.parentFile.listFiles { _, name ->
            name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java"))
        } ?: emptyArray()

        val allFiles = (extraFiles + testDataFile).associateBy { fixture.configureByFile(it.name) }
        val fileWithCaret = allFiles.values.singleOrNull { "<caret>" in it.readText() } ?: error("Must have one <caret>")
        val file = myFixture.configureByFile(fileWithCaret.name)

        withCustomCompilerOptions(file.text, project, module) {
            val afterFileExists = afterFile.exists()
            val targetElement = TargetElementUtil.findTargetElement(
                myFixture.editor,
                ELEMENT_NAME_ACCEPTED or REFERENCED_ELEMENT_ACCEPTED
            )

            val handler = if (targetElement != null)
                InlineActionHandler.EP_NAME.extensions.firstOrNull { it.canInlineElement(targetElement) }
            else
                null

            val expectedErrors = InTextDirectivesUtils.findLinesWithPrefixesRemoved(myFixture.file.text, "// ERROR: ")
            if (handler != null) {
                try {
                    runWriteAction { handler.inlineElement(myFixture.project, myFixture.editor, targetElement) }
                    for ((extraPsiFile, extraFile) in allFiles) {
                        KotlinTestUtils.assertEqualsToFile(File("${extraFile.path}.after"), extraPsiFile.text)
                    }
                } catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
                    TestCase.assertFalse("Refactoring not available: ${e.message}", afterFileExists)
                    TestCase.assertEquals("Expected errors", 1, expectedErrors.size)
                    TestCase.assertEquals("Error message", expectedErrors[0].replace("\\n", "\n"), e.message)
                } catch (e: BaseRefactoringProcessor.ConflictsInTestsException) {
                    TestCase.assertFalse("Conflicts: ${e.message}", afterFileExists)
                    TestCase.assertEquals("Expected errors", 1, expectedErrors.size)
                    TestCase.assertEquals("Error message", expectedErrors[0].replace("\\n", "\n"), e.message)
                }
            } else {
                TestCase.assertFalse("No refactoring handler available", afterFileExists)
            }
        }
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
