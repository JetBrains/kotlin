/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File
import java.io.IOException

abstract class AbstractQuickFixTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    protected fun doTest(beforeFileName: String) {
        val beforeFileText = FileUtil.loadFile(File(beforeFileName))
        configureLanguageVersion(beforeFileText, project, module)

        enableInspections(beforeFileName, beforeFileText)

        doKotlinQuickFixTest(beforeFileName)
        checkForUnexpectedErrors()
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if ("createfromusage" in testDataPath.toLowerCase()) {
            return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        }
        return super.getProjectDescriptor()
    }

    override val captureExceptions: Boolean
        get() = false

    fun shouldBeAvailableAfterExecution(): Boolean {
        return InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION")
    }

    protected open fun configExtra(options: String) {

    }

    private fun getPathAccordingToPackage(name: String, text: String): String {
        val packagePath = text.lines().let { it.find { it.trim().startsWith("package") } }
                                  ?.removePrefix("package")
                                  ?.trim()?.replace(".", "/") ?: ""
        return packagePath + "/" + name
    }

    private fun doKotlinQuickFixTest(beforeFileName: String) {
        val testFile = File(beforeFileName)
        CommandProcessor.getInstance().executeCommand(project, {
            var fileText = ""
            var expectedErrorMessage: String? = ""
            var fixtureClasses = emptyList<String>()
            try {
                fileText = FileUtil.loadFile(testFile, CharsetToolkit.UTF8_CHARSET)
                TestCase.assertTrue("\"<caret>\" is missing in file \"${testFile.path}\"", fileText.contains("<caret>"))

                fixtureClasses = InTextDirectivesUtils.findListWithPrefixes(fileText, "// FIXTURE_CLASS: ")
                for (fixtureClass in fixtureClasses) {
                    TestFixtureExtension.loadFixture(fixtureClass, LightPlatformTestCase.getModule())
                }

                expectedErrorMessage = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ")
                val contents = StringUtil.convertLineSeparators(fileText)
                var filePath = testFile.canonicalFile.name
                val putIntoPackageFolder = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FORCE_PACKAGE_FOLDER") != null
                if (putIntoPackageFolder) {
                    filePath = getPathAccordingToPackage(filePath, contents)
                    myFixture.addFileToProject(filePath, contents)
                    myFixture.configureByFile(filePath)
                }
                else {
                    myFixture.configureByText(filePath, contents)
                }

                checkForUnexpectedActions()

                configExtra(fileText)

                applyAction(contents, filePath)

                UsefulTestCase.assertEmpty(expectedErrorMessage)
            }
            catch (e: FileComparisonFailure) {
                throw e
            }
            catch (e: AssertionError) {
                throw e
            }
            catch (e: Throwable) {
                if (expectedErrorMessage == null) {
                    throw e
                }
                else {
                    Assert.assertEquals("Wrong exception message", expectedErrorMessage, e.message)
                }
            }
            finally {
                for (fixtureClass in fixtureClasses) {
                    TestFixtureExtension.unloadFixture(fixtureClass)
                }
                ConfigLibraryUtil.unconfigureLibrariesByDirective(myFixture.module, fileText)
            }
        }, "", "")
    }

    private fun applyAction(contents: String, testFullPath: String) {
        val fileName = testFullPath.substringAfterLast(File.separatorChar, "")
        val actionHint = ActionHint.parse(myFixture.file, contents.replace("\${file}", fileName, ignoreCase = true))
        val intention = findActionWithText(actionHint.expectedText)
        if (actionHint.shouldPresent()) {
            if (intention == null) {
                fail("Action with text '" + actionHint.expectedText + "' not found\nAvailable actions: " +
                     myFixture.availableIntentions.joinToString(prefix = "[", postfix = "]") { it.text })
            }
            myFixture.launchAction(intention!!)
            UIUtil.dispatchAllInvocationEvents()
            UIUtil.dispatchAllInvocationEvents()

            if (!shouldBeAvailableAfterExecution()) {
                assertNull("Action '${actionHint.expectedText}' is still available after its invocation in test " + testFullPath,
                            findActionWithText(actionHint.expectedText))
            }

            myFixture.checkResultByFile(File(testFullPath).name + ".after")
        }
        else {
            assertNull("Action with text ${actionHint.expectedText} is present, but should not", intention)
        }
    }

    private fun enableInspections(beforeFileName: String, beforeFileText: String) {
        val toolsStrings = InTextDirectivesUtils.findListWithPrefixes(beforeFileText, "TOOL:")
        if (toolsStrings.isNotEmpty()) {
            val inspections =  toolsStrings.map { toolFqName ->
                try {
                    val aClass = Class.forName(toolFqName)
                    return@map aClass.newInstance() as LocalInspectionTool
                }
                catch (e: Exception) {
                    throw IllegalArgumentException("Failed to create inspection for key '$toolFqName'", e)
                }
            }
            myFixture.enableInspections(*inspections.toTypedArray())
            return
        }

        val inspectionFile = findInspectionFile(File(beforeFileName).parentFile)
        if (inspectionFile != null) {
            val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
            val inspectionClass = Class.forName(className) as Class<InspectionProfileEntry>
            val tools = InspectionTestUtil.instantiateTools(
                    listOf<Class<out InspectionProfileEntry>>(inspectionClass))
            myFixture.enableInspections(*tools.toTypedArray())
        }
    }

    @Throws(ClassNotFoundException::class)
    private fun checkForUnexpectedActions() {
        val text = myFixture.editor.document.text
        val actionHint = ActionHint.parse(myFixture.file, text)
        if (!actionHint.shouldPresent()) {
            val actions = myFixture.availableIntentions

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
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected(myFixture.file, actions)
            }
        }
    }

    fun findActionWithText(text: String): IntentionAction? {
        val intentions = myFixture.availableIntentions.filter { it.text == text }
        if (intentions.isNotEmpty()) return intentions.first()

        // Support warning suppression
        val caretOffset = myFixture.caretOffset
        for (highlight in myFixture.doHighlighting()) {
            if (highlight.startOffset <= caretOffset && caretOffset <= highlight.endOffset) {
                val group = highlight.problemGroup
                if (group is SuppressableProblemGroup) {
                    val at = myFixture.file.findElementAt(highlight.actualStartOffset)
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

    fun checkForUnexpectedErrors() {
        DirectiveBasedActionUtils.checkForUnexpectedErrors(myFixture.file as KtFile)
    }

    override fun getTestDataPath(): String {
        // Ensure full path is returned. Otherwise FileComparisonFailureException does not provide link to file diff
        val testDataPath = super.getTestDataPath()
        try {
            return File(testDataPath).getCanonicalPath()
        }
        catch (e: IOException) {
            e.printStackTrace()
            return testDataPath
        }

    }
}
