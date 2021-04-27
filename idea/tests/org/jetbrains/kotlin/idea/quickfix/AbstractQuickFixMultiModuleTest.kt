/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.testFramework.UsefulTestCase
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.Assert
import java.io.File

abstract class AbstractQuickFixMultiModuleTest : AbstractMultiModuleTest(), QuickFixTest {

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/multiModuleQuickFix/"

    fun doTest(dirPath: String) {
        setupMppProjectFromDirStructure(File(dirPath))
        val directiveFileText = project.findFileWithCaret().text
        withCustomCompilerOptions(directiveFileText, project, module) {
            doQuickFixTest(dirPath)
        }
    }

    private fun doQuickFixTest(dirPath: String) {
        val actionFile = project.findFileWithCaret()
        val virtualFile = actionFile.virtualFile!!
        configureByExistingFile(virtualFile)
        val actionFileText = actionFile.text
        val actionFileName = actionFile.name
        val inspections = parseInspectionsToEnable(virtualFile.path, actionFileText).toTypedArray()
        enableInspectionTools(*inspections)

        project.executeCommand("") {
            var expectedErrorMessage = ""
            try {
                val actionHint = ActionHint.parse(actionFile, actionFileText)
                val text = actionHint.expectedText

                val actionShouldBeAvailable = actionHint.shouldPresent()

                expectedErrorMessage = InTextDirectivesUtils.findListWithPrefixes(
                    actionFileText,
                    "// SHOULD_FAIL_WITH: "
                ).joinToString(separator = "\n")

                TypeAccessibilityChecker.testLog = StringBuilder()
                val log = try {
                    AbstractQuickFixMultiFileTest.doAction(
                        text,
                        file,
                        editor,
                        actionShouldBeAvailable,
                        actionFileName,
                        this::availableActions,
                        this::doHighlighting,
                        InTextDirectivesUtils.isDirectiveDefined(actionFile.text, "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION")
                    )

                    TypeAccessibilityChecker.testLog.toString()
                } finally {
                    TypeAccessibilityChecker.testLog = null
                }

                if (actionFile is KtFile) {
                    DirectiveBasedActionUtils.checkForUnexpectedErrors(actionFile)
                }

                if (actionShouldBeAvailable) {
                    compareToExpected(dirPath)
                }

                UsefulTestCase.assertEmpty(expectedErrorMessage)
                val logFile = File("${dirPath}log.log")
                if (log.isNotEmpty()) {
                    KotlinTestUtils.assertEqualsToFile(logFile, log)
                } else {
                    TestCase.assertFalse(logFile.exists())
                }

            } catch (e: ComparisonFailure) {
                throw e
            } catch (e: AssertionError) {
                throw e
            } catch (e: Throwable) {
                if (expectedErrorMessage.isEmpty()) {
                    e.printStackTrace()
                    TestCase.fail(getTestName(true))
                } else {
                    Assert.assertEquals("Wrong exception message", expectedErrorMessage, e.message)
                    compareToExpected(dirPath)
                }
            }
        }
    }

    private fun compareToExpected(directory: String) {
        val projectDirectory = File("${KtTestUtil.getHomeDirectory()}/$directory")
        val afterFiles = projectDirectory.walkTopDown().filter { it.path.endsWith(".after") }.toList()

        for (editedFile in project.allKotlinFiles()) {
            val afterFileInTmpProject = editedFile.containingDirectory?.findFile(editedFile.name + ".after") ?: continue
            val afterFileInTestData = afterFiles.filter { it.name == afterFileInTmpProject.name }.single {
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
