/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractQuickFixMultiModuleTest : AbstractMultiModuleTest() {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleQuickFix/"

    protected fun doQuickFixTest() {
        val allFilesInProject = project.allKotlinFiles()
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
                        val dirName = moduleDirectory.name
                        for (file in moduleDirectory.walkTopDown()) {
                            if (!file.path.endsWith(".after")) continue
                            try {
                                val packageName = file.readLines().find { it.startsWith("package") }?.substringAfter(" ") ?: "<root>"
                                val editedFile = allFilesInProject.mapNotNull {
                                    val candidate = it.containingDirectory?.findFile(file.name.removeSuffix(".after")) as? KtFile
                                    val isUnderTestRoot = candidate != null && "// TEST" in candidate.text
                                    candidate?.takeIf {
                                        it.packageFqName.toString() == packageName &&
                                        it.module?.let { module ->
                                            module.name + ("Test".takeIf { isUnderTestRoot } ?: "") == dirName
                                        } == true
                                    }
                                }.singleOrNull() ?: error("Cannot find suitable candidate for file ${file.name} from directory $dirName")
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
