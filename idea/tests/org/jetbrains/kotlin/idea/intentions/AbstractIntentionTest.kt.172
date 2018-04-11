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

package org.jetbrains.kotlin.idea.intentions

import com.google.common.collect.Lists
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiFile
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.PlatformTestUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

abstract class AbstractIntentionTest : KotlinLightCodeInsightFixtureTestCase() {
    protected open fun intentionFileName(): String = ".intention"

    protected open fun afterFileNameSuffix(): String = ".after"

    protected open fun isApplicableDirectiveName(): String = "IS_APPLICABLE"

    protected open fun intentionTextDirectiveName(): String = "INTENTION_TEXT"

    @Throws(Exception::class)
    private fun createIntention(testDataFile: File): IntentionAction {
        val candidateFiles = Lists.newArrayList<File>()

        var current: File? = testDataFile.parentFile
        while (current != null) {
            val candidate = File(current, intentionFileName())
            if (candidate.exists()) {
                candidateFiles.add(candidate)
            }
            current = current.parentFile
        }

        if (candidateFiles.isEmpty()) {
            throw AssertionError(".intention file is not found for " + testDataFile +
                                 "\nAdd it to base directory of test data. It should contain fully-qualified name of intention class.")
        }
        if (candidateFiles.size > 1) {
            throw AssertionError("Several .intention files are available for " + testDataFile +
                                 "\nPlease remove some of them\n" + candidateFiles)
        }

        val className = FileUtil.loadFile(candidateFiles[0]).trim { it <= ' ' }
        return Class.forName(className).newInstance() as IntentionAction
    }

    @Throws(Exception::class)
    protected fun doTest(path: String) {
        val mainFile = File(path)
        val mainFileName = FileUtil.getNameWithoutExtension(mainFile)
        val intentionAction = createIntention(mainFile)
        val sourceFilePaths = ArrayList<String>()
        val parentDir = mainFile.parentFile
        var i = 1
        sourceFilePaths.add(mainFile.name)
        extraFileLoop@ while (true) {
            for (extension in EXTENSIONS) {
                val extraFile = File(parentDir, mainFileName + "." + i + extension)
                if (extraFile.exists()) {
                    sourceFilePaths.add(extraFile.name)
                    i++
                    continue@extraFileLoop
                }
            }
            break
        }

        val psiFiles = myFixture.configureByFiles(*sourceFilePaths.toTypedArray())
        val pathToFiles = mapOf(*(sourceFilePaths zip psiFiles).toTypedArray())

        val fileText = FileUtil.loadFile(mainFile, true)

        ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

        try {
            TestCase.assertTrue("\"<caret>\" is missing in file \"$mainFile\"", fileText.contains("<caret>"))

            val minJavaVersion = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MIN_JAVA_VERSION: ")
            if (minJavaVersion != null && !SystemInfo.isJavaVersionAtLeast(minJavaVersion)) return

            if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_BEFORE")) {
                DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
            }

            doTestFor(mainFile.name, pathToFiles, intentionAction, fileText)

            if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_AFTER")) {
                DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
            }
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
        }
    }

    private fun <T> computeUnderProgressIndicatorAndWait(compute: () -> T): T {
        val result = CompletableFuture<T>()
        val progressIndicator = ProgressIndicatorBase()
        try {
            val task = object : Task.Backgroundable(project, "isApplicable", false) {
                override fun run(indicator: ProgressIndicator) {
                    result.complete(compute())
                }
            }
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator)
            return result.get(10, TimeUnit.SECONDS)
        }
        finally {
            progressIndicator.cancel()
        }
    }

    @Throws(Exception::class)
    private fun doTestFor(mainFilePath: String, pathToFiles: Map<String, PsiFile>, intentionAction: IntentionAction, fileText: String) {
        val isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// ${isApplicableDirectiveName()}: ")
        val isApplicableExpected = isApplicableString == null || isApplicableString == "true"

        val isApplicableOnPooled = computeUnderProgressIndicatorAndWait { ApplicationManager.getApplication().runReadAction(Computable { intentionAction.isAvailable(project, editor, file) }) }

        val isApplicableOnEdt = intentionAction.isAvailable(project, editor, file)

        Assert.assertEquals("There should not be any difference what thread isApplicable is called from", isApplicableOnPooled, isApplicableOnEdt)

        Assert.assertTrue(
                "isAvailable() for " + intentionAction.javaClass + " should return " + isApplicableExpected,
                isApplicableExpected == isApplicableOnEdt)

        val intentionTextString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// " + intentionTextDirectiveName() + ": ")

        if (intentionTextString != null) {
            TestCase.assertEquals("Intention text mismatch.", intentionTextString, intentionAction.text)
        }

        val shouldFailString = StringUtil.join(InTextDirectivesUtils.findListWithPrefixes(fileText, "// SHOULD_FAIL_WITH: "), ", ")

        try {
            if (isApplicableExpected) {
                project.executeWriteCommand(
                        intentionAction.text, null
                ) {
                    intentionAction.invoke(project, editor, file)
                    null
                }
                // Don't bother checking if it should have failed.
                if (shouldFailString.isEmpty()) {
                    for ((filePath, value) in pathToFiles) {
                        val canonicalPathToExpectedFile = filePath + afterFileNameSuffix()
                        if (filePath == mainFilePath) {
                            try {
                                myFixture.checkResultByFile(canonicalPathToExpectedFile)
                            }
                            catch (e: ComparisonFailure) {
                                KotlinTestUtils
                                        .assertEqualsToFile(File(testDataPath, canonicalPathToExpectedFile), editor.document.text)
                            }

                        }
                        else {
                            KotlinTestUtils.assertEqualsToFile(File(testDataPath, canonicalPathToExpectedFile), value.text)
                        }
                    }
                }
            }
            TestCase.assertEquals("Expected test to fail.", "", shouldFailString)
        }
        catch (e: BaseRefactoringProcessor.ConflictsInTestsException) {
            TestCase.assertEquals("Failure message mismatch.", shouldFailString, StringUtil.join(e.messages.sorted(), ", "))
        }
        catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
            TestCase.assertEquals("Failure message mismatch.", shouldFailString, e.message?.replace('\n', ' '))
        }
    }

    companion object {
        private val EXTENSIONS = arrayOf(".kt", ".kts", ".java", ".groovy")
    }
}

