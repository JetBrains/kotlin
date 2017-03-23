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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.util.regex.Pattern

abstract class AbstractQuickFixMultiModuleTest : AbstractMultiModuleTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleQuickFix/"

    protected fun shouldBeAvailableAfterExecution(file: KtFile) =
            InTextDirectivesUtils.isDirectiveDefined(file.text, "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION")

    private fun getActionsTexts(availableActions: List<IntentionAction>) = availableActions.map { it.text }

    protected fun doQuickFixTest() {
        val allFilesInProject = PluginJetFilesProvider.allFilesInProject(myProject!!)
        val actionFile = allFilesInProject.single { file ->
            file.text.contains("// \"")
        }

        configureByExistingFile(actionFile.virtualFile!!)

        val actionFileText = actionFile.text
        val actionFileName = actionFile.name

        CommandProcessor.getInstance().executeCommand(project, {
            try {
                val psiFile = actionFile

                val pair = LightQuickFixTestCase.parseActionHint(psiFile, actionFileText)
                val text = pair.getFirst()

                val actionShouldBeAvailable = pair.getSecond()

                if (psiFile is KtFile) {
                    DirectiveBasedActionUtils.checkForUnexpectedErrors(psiFile)
                }

                doAction(text, actionShouldBeAvailable, actionFileName, actionFile)

                if (actionShouldBeAvailable) {
                    val testDirectory = File(testDataPath)
                    val projectDirectory = File("$testDataPath${getTestName(true)}")
                    for (moduleDirectory in projectDirectory.listFiles()) {
                        for (file in moduleDirectory.walkTopDown()) {
                            if (!file.path.endsWith(".after")) continue
                            try {
                                val packageName = file.readLines().find { it.startsWith("package") }?.substringAfter(" ") ?: "<root>"
                                val editedFile = allFilesInProject.mapNotNull {
                                    val candidate = it.containingDirectory?.findFile(file.name.removeSuffix(".after")) as? KtFile
                                    if (candidate?.packageFqName?.toString() == packageName) candidate else null
                                }.single()
                                setActiveEditor(editedFile.findExistingEditor() ?: createEditor(editedFile.virtualFile))
                                checkResultByFile(file.relativeTo(testDirectory).path)
                            }
                            catch (e: ComparisonFailure) {
                                KotlinTestUtils.assertEqualsToFile(file, editor)
                            }
                        }
                    }
                }
            }
            catch (e: ComparisonFailure) {
                throw e
            }
            catch (e: AssertionError) {
                throw e
            }
            catch (e: Throwable) {
                e.printStackTrace()
                TestCase.fail(getTestName(true))
            }
        }, "", "")
    }

    // TODO: merge with AbstractQuickFixMultiFileTest

    fun doAction(text: String, actionShouldBeAvailable: Boolean, testFilePath: String, actionFile: KtFile) {
        val pattern = if (text.startsWith("/"))
            Pattern.compile(text.substring(1, text.length - 1))
        else
            Pattern.compile(StringUtil.escapeToRegexp(text))

        val availableActions = getAvailableActions()
        val action = findActionByPattern(pattern, availableActions)

        if (action == null) {
            if (actionShouldBeAvailable) {
                val texts = getActionsTexts(availableActions)
                val infos = doHighlighting()
                TestCase.fail("Action with text '" + text + "' is not available in test " + testFilePath + "\n" +
                              "Available actions (" + texts.size + "): \n" +
                              StringUtil.join(texts, "\n") +
                              "\nActions:\n" +
                              StringUtil.join(availableActions, "\n") +
                              "\nInfos:\n" +
                              StringUtil.join(infos, "\n"))
            }
            else {
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected(file, availableActions)
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                TestCase.fail("Action '$text' is available (but must not) in test $testFilePath")
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(file, editor, action, action.text)

            UIUtil.dispatchAllInvocationEvents()


            if (!shouldBeAvailableAfterExecution(actionFile)) {
                val afterAction = findActionByPattern(pattern, getAvailableActions())

                if (afterAction != null) {
                    TestCase.fail("Action '$text' is still available after its invocation in test $testFilePath")
                }
            }
        }
    }

    private fun findActionByPattern(pattern: Pattern, availableActions: List<IntentionAction>): IntentionAction? {
        for (availableAction in availableActions) {
            if (pattern.matcher(availableAction.text).matches()) {
                return availableAction
            }
        }
        return null
    }

    private fun getAvailableActions(): List<IntentionAction> {
        doHighlighting()
        return LightQuickFixTestCase.getAvailableActions(editor, file)
    }

    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()
}
