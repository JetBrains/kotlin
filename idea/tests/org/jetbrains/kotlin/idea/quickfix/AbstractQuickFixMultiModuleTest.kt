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

import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
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

abstract class AbstractQuickFixMultiModuleTest : AbstractMultiModuleTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleQuickFix/"

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
                val actionHint = ActionHint.parse(actionFile, actionFileText)
                val text = actionHint.expectedText

                val actionShouldBeAvailable = actionHint.shouldPresent()

                if (actionFile is KtFile) {
                    DirectiveBasedActionUtils.checkForUnexpectedErrors(actionFile)
                }

                AbstractQuickFixMultiFileTest.doAction(
                        text, file, editor, actionShouldBeAvailable, actionFileName, this::availableActions, this::doHighlighting,
                        InTextDirectivesUtils.isDirectiveDefined(actionFile.text, "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION")
                )

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

    private val availableActions: List<IntentionAction>
        get() {
            doHighlighting()
            return LightQuickFixTestCase.getAvailableActions(editor, file)
        }

    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()
}
