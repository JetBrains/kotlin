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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.daemon.quickFix.QuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.stubs.StubUpdatingIndex
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ArrayUtil
import com.intellij.util.ObjectUtils.notNull
import com.intellij.util.indexing.FileBasedIndex
import junit.framework.TestCase
import org.apache.commons.lang.SystemUtils
import org.jetbrains.kotlin.idea.KotlinLightQuickFixTestCase
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.TestFixtureExtension
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

abstract class AbstractQuickFixTest : KotlinLightQuickFixTestCase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        (StartupManager.getInstance(LightPlatformTestCase.getProject()) as StartupManagerImpl).runPostStartupActivities()
    }

    @Throws(Exception::class)
    protected fun doTest(beforeFileName: String) {
        try {
            configureRuntimeIfNeeded(beforeFileName)

            enableInspections(beforeFileName)

            doSingleTest(getTestName(false) + ".kt")
            checkForUnexpectedErrors()
        }
        finally {
            unConfigureRuntimeIfNeeded(beforeFileName)
        }
    }

    //region Severe hack - lot of code copied from LightQuickFixTestCase to workaround stupid format of test data with before/after prefixes
    override fun doSingleTest(fileSuffix: String) {
        doKotlinQuickFixTest(fileSuffix, createWrapper())
    }

    override fun shouldBeAvailableAfterExecution(): Boolean {
        return InTextDirectivesUtils.isDirectiveDefined(myWrapper!!.file.text, "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION")
    }

    override fun configureLocalInspectionTools(): Array<LocalInspectionTool> {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.configureLocalInspectionTools()

        val testRoot = KotlinTestUtils.getTestsRoot(this.javaClass)
        val configFileText = File(testRoot, getTestName(true) + ".kt").readText(Charset.defaultCharset())
        val toolsStrings = InTextDirectivesUtils.findListWithPrefixes(configFileText, "TOOL:")

        if (toolsStrings.isEmpty()) return super.configureLocalInspectionTools()

        return ArrayUtil.toObjectArray(toolsStrings.map { toolFqName ->
            try {
                val aClass = Class.forName(toolFqName)
                return@map aClass.newInstance() as LocalInspectionTool
            }
            catch (e: Exception) {
                throw IllegalArgumentException("Failed to create inspection for key '$toolFqName'", e)
            }
        }, LocalInspectionTool::class.java)
    }

    protected open fun configExtra(options: String) {

    }

    private fun doKotlinQuickFixTest(testName: String, quickFixTestCase: QuickFixTestCase) {
        val relativePath = notNull(quickFixTestCase.basePath, "") + "/" + testName.decapitalize()
        val testFullPath = quickFixTestCase.testDataPath.replace(File.separatorChar, '/') + relativePath
        val testFile = File(testFullPath)
        CommandProcessor.getInstance().executeCommand(quickFixTestCase.project, {
            var fileText = ""
            var expectedErrorMessage: String? = ""
            var fixtureClasses = emptyList<String>()
            try {
                fileText = FileUtil.loadFile(testFile, CharsetToolkit.UTF8_CHARSET)
                TestCase.assertTrue("\"<caret>\" is missing in file \"$testName\"", fileText.contains("<caret>"))

                fixtureClasses = InTextDirectivesUtils.findListWithPrefixes(fileText, "// FIXTURE_CLASS: ")
                for (fixtureClass in fixtureClasses) {
                    TestFixtureExtension.loadFixture(fixtureClass, LightPlatformTestCase.getModule())
                }

                expectedErrorMessage = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ")
                val contents = StringUtil.convertLineSeparators(fileText)
                quickFixTestCase.configureFromFileText(testFile.name, contents)
                quickFixTestCase.bringRealEditorBack()

                checkForUnexpectedActions()

                configExtra(fileText)

                applyAction(contents, quickFixTestCase, testName, testFullPath)

                UsefulTestCase.assertEmpty(expectedErrorMessage)
            }
            catch (e: FileComparisonFailure) {
                throw e
            }
            catch (e: AssertionError) {
                throw e
            }
            catch (e: Throwable) {
                if (expectedErrorMessage == null || expectedErrorMessage != e.message) {
                    e.printStackTrace()
                    TestCase.fail(testName)
                }
            }
            finally {
                for (fixtureClass in fixtureClasses) {
                    TestFixtureExtension.unloadFixture(fixtureClass)
                }
                ConfigLibraryUtil.unconfigureLibrariesByDirective(LightPlatformTestCase.getModule(), fileText)
            }
        }, "", "")
    }

    @Throws(Exception::class)
    override fun doAction(actionHint: ActionHint, testFullPath: String, testName: String) {
        LightQuickFixTestCase.doAction(actionHint, testFullPath, testName, myWrapper!!)
    }

    override fun checkResultByFile(message: String?, filePath: String, ignoreTrailingSpaces: Boolean) {
        val file = File(filePath)
        val afterFileName = file.name
        assert(afterFileName.startsWith(LightQuickFixTestCase.AFTER_PREFIX))
        val newAfterFileName = afterFileName.substring(LightQuickFixTestCase.AFTER_PREFIX.length).decapitalize() + ".after"

        super.checkResultByFile(message, File(file.parent, newAfterFileName).path, ignoreTrailingSpaces)
    }

    @Throws(IOException::class)
    private fun unConfigureRuntimeIfNeeded(beforeFileName: String) {
        if (beforeFileName.endsWith("JsRuntime.kt")) {
            ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(LightPlatformTestCase.getModule(), projectJDK)
        }
        else if (isRuntimeNeeded(beforeFileName)) {
            ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(LightPlatformTestCase.getModule(), projectJDK)
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun enableInspections(beforeFileName: String) {
        val inspectionFile = findInspectionFile(File(beforeFileName).parentFile)
        if (inspectionFile != null) {
            val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
            val inspectionClass = Class.forName(className) as Class<InspectionProfileEntry>
            val tools = InspectionTestUtil.instantiateTools(
                    listOf<Class<out InspectionProfileEntry>>(inspectionClass))
            enableInspectionTools(tools[0])
        }
    }

    @Throws(ClassNotFoundException::class)
    private fun checkForUnexpectedActions() {
        val text = LightPlatformCodeInsightTestCase.getEditor().getDocument().getText()
        val actionHint = ActionHint.parse(LightPlatformCodeInsightTestCase.getFile(), text)
        if (!actionHint.shouldPresent()) {
            val actions = availableActions

            val prefix = "class "
            if (actionHint.expectedText.startsWith(prefix)) {
                val className = actionHint.expectedText.substring(prefix.length)
                val aClass = Class.forName(className)
                assert(IntentionAction::class.java.isAssignableFrom(aClass)) { className + " should be inheritor of IntentionAction" }

                val validActions = HashSet(InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ACTION:"))

                actions.removeAll { action -> !aClass.isAssignableFrom(action.javaClass) || validActions.contains(action.text) }

                if (!actions.isEmpty()) {
                    Assert.fail("Unexpected intention actions present\n " + actions.map { action -> action.javaClass.toString() + " " + action.toString() + "\n" }
                    )
                }

                for (action in actions) {
                    if (aClass.isAssignableFrom(action.javaClass) && !validActions.contains(action.text)) {
                        Assert.fail("Unexpected intention action " + action.javaClass + " found")
                    }
                }
            }
            else {
                // Action shouldn't be found. Check that other actions are expected and thus tested action isn't there under another name.
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected(LightPlatformCodeInsightTestCase.getFile(), actions)
            }
        }
    }

    override fun findActionWithText(text: String): IntentionAction? {
        val intention = super.findActionWithText(text)
        if (intention != null) return intention

        // Support warning suppression
        val caretOffset = LightPlatformCodeInsightTestCase.myEditor.getCaretModel().getOffset()
        for (highlight in doHighlighting()) {
            if (highlight.startOffset <= caretOffset && caretOffset <= highlight.endOffset) {
                val group = highlight.problemGroup
                if (group is SuppressableProblemGroup) {
                    val at = LightPlatformCodeInsightTestCase.getFile().findElementAt(highlight.actualStartOffset)
                    val actions = group.getSuppressActions(at)
                    for (action in actions) {
                        if (action.text == text) {
                            return action
                        }
                    }
                }
            }
        }
        return null
    }

    override fun checkResultByText(message: String?, fileText: String, ignoreTrailingSpaces: Boolean, filePath: String?) {
        super.checkResultByText(message, fileText, ignoreTrailingSpaces, File(filePath!!).absolutePath)
    }

    override fun getBasePath(): String {
        return KotlinTestUtils.getTestsRoot(javaClass)
    }

    override fun getTestDataPath(): String {
        return "./"
    }

    override fun getProjectJDK(): Sdk {
        return PluginTestCaseBase.mockJdk()
    }

    companion object {

        private var myWrapper: QuickFixTestCase? = null

        @Throws(Exception::class)
        private fun applyAction(contents: String, quickFixTestCase: QuickFixTestCase, testName: String, testFullPath: String) {
            val fileName = testFullPath.substringAfterLast("/", "")
            val actionHint = ActionHint.parse(quickFixTestCase.file, contents.replace("\${file}", fileName))

            quickFixTestCase.beforeActionStarted(testName, contents)

            try {
                myWrapper = quickFixTestCase
                quickFixTestCase.doAction(actionHint, testFullPath, testName)
            }
            finally {
                myWrapper = null
                quickFixTestCase.afterActionCompleted(testName, contents)
            }
        }
        //endregion

        @Throws(IOException::class)
        private fun configureRuntimeIfNeeded(beforeFileName: String) {
            if (beforeFileName.endsWith("JsRuntime.kt")) {
                // Without the following line of code subsequent tests with js-runtime will be prone to failure due "outdated stub in index" error.
                FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID)

                ConfigLibraryUtil.configureKotlinJsRuntimeAndSdk(LightPlatformTestCase.getModule(), fullJavaJDK)
            }
            else if (isRuntimeNeeded(beforeFileName)) {
                ConfigLibraryUtil.configureKotlinRuntimeAndSdk(LightPlatformTestCase.getModule(), fullJavaJDK)
            }
            else if (beforeFileName.contains("Runtime") || beforeFileName.contains("JsRuntime")) {
                Assert.fail("Runtime marker is used in test name, but not in test file end. " + "This can lead to false-positive absent of actions")
            }
        }

        @Throws(IOException::class)
        private fun isRuntimeNeeded(beforeFileName: String): Boolean {
            return beforeFileName.endsWith("Runtime.kt") ||
                   beforeFileName.toLowerCase().contains("createfromusage") ||
                   InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(File(beforeFileName)), "WITH_RUNTIME")
        }

        fun checkForUnexpectedErrors() {
            DirectiveBasedActionUtils.checkForUnexpectedErrors(LightPlatformCodeInsightTestCase.getFile() as KtFile)
        }

        protected val fullJavaJDK: Sdk
            get() = JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().absolutePath)
    }
}
