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
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.allKotlinFiles
import org.jetbrains.kotlin.idea.test.findFileWithCaret
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractQuickFixMultiModuleTest : AbstractMultiModuleTest(), QuickFixTest {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleQuickFix/"

    fun doTest(dirPath: String) {
        setupMppProjectFromDirStructure(File(dirPath))
        doQuickFixTest()
    }

    private fun doQuickFixTest() {
        val actionFile = project.findFileWithCaret()
        val virtualFile = actionFile.virtualFile!!
        configureByExistingFile(virtualFile)
        val actionFileText = actionFile.text
        val actionFileName = actionFile.name
        val inspections = parseInspectionsToEnable(virtualFile.path, actionFileText).toTypedArray()
        enableInspectionTools(*inspections)

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
                    compareToExpected()
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

    private fun compareToExpected() {
        val projectDirectory = File("$testDataPath${getTestName(true)}")
        val afterFiles = projectDirectory.walkTopDown().filter { it.path.endsWith(".after") }.toList()

        for (editedFile in project.allKotlinFiles()) {
            val afterFileInTmpProject = editedFile.containingDirectory?.findFile(editedFile.name + ".after") ?: continue
            val afterFileInTestData = afterFiles.filter { it.name == afterFileInTmpProject.name } .single {
                it.readText() == File(afterFileInTmpProject.virtualFile.path).readText()
            }

            setActiveEditor(editedFile.findExistingEditor() ?: createEditor(editedFile.virtualFile))
            try {
                checkResultByFile(afterFileInTestData.relativeTo(File(testDataPath)).path)
            } catch (e: ComparisonFailure) {
                KotlinTestUtils.assertEqualsToFile(afterFileInTestData, editor)
            }
        }
    }

    private val availableActions: List<IntentionAction>
        get() {
            doHighlighting()
            return LightQuickFixTestCase.getAvailableActions(editor, file)
        }

    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()
}
