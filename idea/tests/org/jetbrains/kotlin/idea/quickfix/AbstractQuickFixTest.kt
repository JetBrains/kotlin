/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionDelegate
import com.intellij.codeInsight.intention.impl.config.IntentionActionWrapper
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.codeInspection.ex.QuickFixWrapper
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.caches.resolve.ResolveInDispatchThreadException
import org.jetbrains.kotlin.idea.caches.resolve.forceCheckForResolveInDispatchThreadInTests
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import org.junit.ComparisonFailure
import java.io.File
import java.io.IOException

abstract class AbstractQuickFixTest : KotlinLightCodeInsightFixtureTestCase(), QuickFixTest {
    companion object {
        private val quickFixesAllowedToResolveInWriteAction = AllowedToResolveUnderWriteActionData(
            "idea/testData/quickfix/allowResolveInWriteAction.txt",
            """
                # Actions that are allowed to resolve in write action. Normally this list shouldn't be extended and eventually should 
                # be dropped. Please consider rewriting a quick-fix and remove resolve from it before adding a new entry to this list.
            """.trimIndent()
        )

        private fun unwrapIntention(action: Any): Any = when (action) {
            is IntentionActionDelegate -> unwrapIntention(action.delegate)
            is IntentionActionWrapper -> unwrapIntention(action.delegate)
            is QuickFixWrapper -> unwrapIntention(action.fix)
            else -> action
        }
    }

    @Throws(Exception::class)
    protected fun doTest(beforeFileName: String) {
        val beforeFileText = FileUtil.loadFile(File(beforeFileName))
        withCustomCompilerOptions(beforeFileText, project, module) {
            val inspections = parseInspectionsToEnable(beforeFileName, beforeFileText).toTypedArray()
            try {
                myFixture.enableInspections(*inspections)

                doKotlinQuickFixTest(beforeFileName)
                checkForUnexpectedErrors()
            } finally {
                myFixture.disableInspections(*inspections)
            }
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor =
        if ("createfromusage" in testDataPath.toLowerCase()) {
            KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
        } else {
            super.getProjectDescriptor()
        }

    override val captureExceptions: Boolean
        get() = false

    private fun shouldBeAvailableAfterExecution(): Boolean = InTextDirectivesUtils.isDirectiveDefined(
        myFixture.file.text,
        "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION"
    )

    protected open fun configExtra(options: String) {

    }

    private fun getPathAccordingToPackage(name: String, text: String): String {
        val packagePath = text.lines().let { list -> list.find { it.trim().startsWith("package") } }
            ?.removePrefix("package")
            ?.trim()?.replace(".", "/") ?: ""
        return "$packagePath/$name"
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
                    TestFixtureExtension.loadFixture(fixtureClass, module)
                }

                expectedErrorMessage = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ")
                val contents = StringUtil.convertLineSeparators(fileText)
                var fileName = testFile.canonicalFile.name
                val putIntoPackageFolder = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FORCE_PACKAGE_FOLDER") != null
                if (putIntoPackageFolder) {
                    fileName = getPathAccordingToPackage(fileName, contents)
                    myFixture.addFileToProject(fileName, contents)
                    myFixture.configureByFile(fileName)
                } else {
                    myFixture.configureByText(fileName, contents)
                }

                checkForUnexpectedActions()

                configExtra(fileText)

                applyAction(contents, fileName)

                val compilerArgumentsAfter = InTextDirectivesUtils.findStringWithPrefixes(fileText, "COMPILER_ARGUMENTS_AFTER: ")
                if (compilerArgumentsAfter != null) {
                    val facetSettings = KotlinFacet.get(module)!!.configuration.settings
                    val compilerSettings = facetSettings.compilerSettings
                    TestCase.assertEquals(compilerArgumentsAfter, compilerSettings?.additionalArguments)
                }

                UsefulTestCase.assertEmpty(expectedErrorMessage)
            } catch (e: FileComparisonFailure) {
                throw e
            } catch (e: AssertionError) {
                throw e
            } catch (e: Throwable) {
                if (expectedErrorMessage == null) {
                    throw e
                } else {
                    Assert.assertEquals("Wrong exception message", expectedErrorMessage, e.message)
                }
            } finally {
                for (fixtureClass in fixtureClasses) {
                    TestFixtureExtension.unloadFixture(fixtureClass)
                }
                ConfigLibraryUtil.unconfigureLibrariesByDirective(myFixture.module, fileText)
            }
        }, "", "")
    }

    private fun applyAction(contents: String, fileName: String) {
        val actionHint = ActionHint.parse(myFixture.file, contents.replace("\${file}", fileName, ignoreCase = true))
        val intention = findActionWithText(actionHint.expectedText)
        if (actionHint.shouldPresent()) {
            if (intention == null) {
                fail(
                    "Action with text '" + actionHint.expectedText + "' not found\nAvailable actions: " +
                            myFixture.availableIntentions.joinToString(prefix = "[", postfix = "]") { it.text })
                return
            }

            val writeActionResolveHandler: () -> Unit = {
                val unwrappedIntention = unwrapIntention(intention)

                val intentionClassName = unwrappedIntention.javaClass.name
                if (!quickFixesAllowedToResolveInWriteAction.isWriteActionAllowed(intentionClassName)) {
                    throw ResolveInDispatchThreadException("Resolve is not allowed under the write action for `$intentionClassName`!")
                }
            }

            val stubComparisonFailure: ComparisonFailure? = try {
                forceCheckForResolveInDispatchThreadInTests(writeActionResolveHandler) {
                    myFixture.launchAction(intention)
                }
                null
            } catch (comparisonFailure: ComparisonFailure) {
                comparisonFailure
            }

            UIUtil.dispatchAllInvocationEvents()
            UIUtil.dispatchAllInvocationEvents()

            if (!shouldBeAvailableAfterExecution()) {
                assertNull(
                    "Action '${actionHint.expectedText}' is still available after its invocation in test " + fileName,
                    findActionWithText(actionHint.expectedText)
                )
            }

            myFixture.checkResultByFile(File(fileName).name + ".after")

            if (stubComparisonFailure != null) {
                throw stubComparisonFailure
            }
        } else {
            assertNull("Action with text ${actionHint.expectedText} is present, but should not", intention)
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
                assert(IntentionAction::class.java.isAssignableFrom(aClass)) { "$className should be inheritor of IntentionAction" }

                val validActions = HashSet(InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ACTION:"))

                actions.removeAll { action -> !aClass.isAssignableFrom(action.javaClass) || validActions.contains(action.text) }

                if (actions.isNotEmpty()) {
                    Assert.fail("Unexpected intention actions present\n " + actions.map { action ->
                        action.javaClass.toString() + " " + action.toString() + "\n"
                    }
                    )
                }

                for (action in actions) {
                    if (aClass.isAssignableFrom(action.javaClass) && !validActions.contains(action.text)) {
                        Assert.fail("Unexpected intention action " + action.javaClass + " found")
                    }
                }
            } else {
                // Action shouldn't be found. Check that other actions are expected and thus tested action isn't there under another name.
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected(myFixture.file, actions)
            }
        }
    }

    private fun findActionWithText(text: String): IntentionAction? {
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

    private fun checkForUnexpectedErrors() = DirectiveBasedActionUtils.checkForUnexpectedErrors(myFixture.file as KtFile)

    override fun getTestDataPath(): String {
        // Ensure full path is returned. Otherwise FileComparisonFailureException does not provide link to file diff
        val testDataPath = super.getTestDataPath()
        return try {
            File(testDataPath).canonicalPath
        } catch (e: IOException) {
            e.printStackTrace()
            testDataPath
        }

    }
}
