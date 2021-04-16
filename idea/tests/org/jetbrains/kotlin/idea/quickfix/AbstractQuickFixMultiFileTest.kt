/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ArrayUtil
import com.intellij.util.PathUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestFiles
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import java.io.File
import java.util.*
import java.util.regex.Pattern

abstract class AbstractQuickFixMultiFileTest : KotlinLightCodeInsightFixtureTestCase() {
    protected open fun doTestWithExtraFile(beforeFileName: String) {
        enableInspections(beforeFileName)

        if (beforeFileName.endsWith(".test")) {
            doMultiFileTest(beforeFileName)
        } else {
            doTest(beforeFileName)
        }
    }

    private fun enableInspections(beforeFileName: String) {
        val inspectionFile = findInspectionFile(File(beforeFileName).parentFile)
        if (inspectionFile != null) {
            val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
            val inspectionClass = Class.forName(className)
            enableInspectionTools(inspectionClass)
        }
    }

    private fun enableInspectionTools(klass: Class<*>) {
        val eps = mutableListOf<InspectionEP>().apply {
            addAll(LocalInspectionEP.LOCAL_INSPECTION.extensions)
            addAll(InspectionEP.GLOBAL_INSPECTION.extensions)
        }

        val tool = eps.firstOrNull { it.implementationClass == klass.name }?.instantiateTool()
            ?: error("Could not find inspection tool for class: $klass")

        myFixture.enableInspections(tool)
    }

    override fun setUp() {
        super.setUp()
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = arrayOf("excludedPackage", "somePackage.ExcludedClass")
    }

    override fun tearDown() {
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY
        super.tearDown()
    }

    /**
     * @param subFiles   subFiles of multiFile test
     * *
     * @param beforeFile will be added last, as subFiles are dependencies of it
     */
    private fun configureMultiFileTest(subFiles: List<TestFile>, beforeFile: TestFile): List<VirtualFile> {
        val vFiles = subFiles.map(this::createTestFile).toMutableList()
        val beforeVFile = createTestFile(beforeFile)
        vFiles.add(beforeVFile)
        myFixture.configureFromExistingVirtualFile(beforeVFile)
        TestCase.assertEquals(guessFileType(beforeFile), myFixture.file.virtualFile.fileType)

        TestCase.assertTrue("\"<caret>\" is probably missing in file \"" + beforeFile.path + "\"", myFixture.editor.caretModel.offset != 0)
        return vFiles
    }

    private fun createTestFile(testFile: TestFile): VirtualFile {
        return runWriteAction {
            val vFile = myFixture.tempDirFixture.createFile(testFile.path)
            vFile.charset = CharsetToolkit.UTF8_CHARSET
            VfsUtil.saveText(vFile, testFile.content)
            vFile
        }
    }

    private fun doMultiFileTest(beforeFileName: String) {
        val multiFileText = FileUtil.loadFile(File(beforeFileName), true)

        val subFiles = TestFiles.createTestFiles(
            "single.kt",
            multiFileText,
            object : TestFiles.TestFileFactoryNoModules<TestFile>() {
                override fun create(fileName: String, text: String, directives: Directives): TestFile {
                    val linesWithoutDirectives = text.lines().filter {
                        !it.startsWith("// LANGUAGE_VERSION") && !it.startsWith("// FILE")
                    }
                    return TestFile(fileName, linesWithoutDirectives.joinToString(separator = "\n"))
                }
            }
        )

        val afterFile = subFiles.firstOrNull { file -> file.path.contains(".after") }
        val beforeFile = subFiles.firstOrNull { file -> file.path.contains(".before") }!!

        subFiles.remove(beforeFile)
        if (afterFile != null) {
            subFiles.remove(afterFile)
        }

        configureMultiFileTest(subFiles, beforeFile)
        withCustomCompilerOptions(multiFileText, project, module) {
            project.executeCommand("") {
                try {
                    val psiFile = file

                    val actionHint = ActionHint.parse(psiFile, beforeFile.content)
                    val text = actionHint.expectedText

                    val actionShouldBeAvailable = actionHint.shouldPresent()

                    if (psiFile is KtFile) {
                        checkForUnexpectedErrors(psiFile)
                    }

                    doAction(
                        text,
                        file,
                        editor,
                        actionShouldBeAvailable,
                        getTestName(false),
                        this::availableActions,
                        myFixture::doHighlighting,
                        checkAvailableActionsAreExpected = this::checkAvailableActionsAreExpected
                    )

                    val actualText = file.text
                    val afterText = StringBuilder(actualText).insert(editor.caretModel.offset, "<caret>").toString()

                    if (actionShouldBeAvailable) {
                        TestCase.assertNotNull(".after file should exist", afterFile)
                        if (afterText != afterFile!!.content) {
                            val actualTestFile = StringBuilder()
                            if (multiFileText.startsWith("// LANGUAGE_VERSION")) {
                                actualTestFile.append(multiFileText.lineSequence().first())
                            }

                            actualTestFile.append("// FILE: ").append(beforeFile.path).append("\n").append(beforeFile.content)
                            for (file in subFiles) {
                                actualTestFile.append("// FILE: ").append(file.path).append("\n").append(file.content)
                            }
                            actualTestFile.append("// FILE: ").append(afterFile.path).append("\n").append(afterText)

                            KotlinTestUtils.assertEqualsToFile(File(beforeFileName), actualTestFile.toString())
                        }
                    } else {
                        TestCase.assertNull(".after file should not exist", afterFile)
                    }
                } catch (e: ComparisonFailure) {
                    throw e
                } catch (e: AssertionError) {
                    throw e
                } catch (e: Throwable) {
                    e.printStackTrace()
                    TestCase.fail(getTestName(true))
                }
            }
        }

    }

    private fun doTest(beforeFileName: String) {
        val mainFile = File(beforeFileName)
        val originalFileText = FileUtil.loadFile(mainFile, true)
        val mainFileDir = mainFile.parentFile!!

        val mainFileName = mainFile.name
        val extraFiles = mainFileDir.listFiles { _, name ->
            name.startsWith(extraFileNamePrefix(mainFileName))
                    && name != mainFileName
                    && PathUtil.getFileExtension(name).let { it == "kt" || it == "java" || it == "groovy" }
        }!!

        val testFiles = ArrayList<String>()
        testFiles.add(mainFile.name)
        extraFiles.mapTo(testFiles) { file -> file.name }

        myFixture.configureByFiles(*testFiles.toTypedArray())

        withCustomCompilerOptions(originalFileText, project, module) {
            project.executeCommand("") {
                try {
                    val psiFile = file

                    val actionHint = ActionHint.parse(psiFile, originalFileText)
                    val text = actionHint.expectedText

                    val actionShouldBeAvailable = actionHint.shouldPresent()

                    if (psiFile is KtFile) {
                        checkForUnexpectedErrors(psiFile)
                    }

                    doAction(
                        text,
                        file,
                        editor,
                        actionShouldBeAvailable,
                        beforeFileName,
                        this::availableActions,
                        myFixture::doHighlighting,
                        checkAvailableActionsAreExpected = this::checkAvailableActionsAreExpected
                    )

                    if (actionShouldBeAvailable) {
                        val afterFilePath = beforeFileName.replace(".before.Main.", ".after.")
                        try {
                            myFixture.checkResultByFile(mainFile.name.replace(".before.Main.", ".after."))
                        } catch (e: ComparisonFailure) {
                            KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
                        }

                        for (file in myFixture.file.containingDirectory.files) {
                            val fileName = file.name
                            if (fileName == myFixture.file.name || !fileName.startsWith(
                                    extraFileNamePrefix(
                                        myFixture.file
                                            .name,
                                    ),
                                )
                            ) continue

                            val extraFileFullPath = beforeFileName.replace(myFixture.file.name, fileName)
                            val afterFile = File(extraFileFullPath.replace(".before.", ".after."))
                            if (afterFile.exists()) {
                                KotlinTestUtils.assertEqualsToFile(afterFile, file.text)
                            } else {
                                KotlinTestUtils.assertEqualsToFile(File(extraFileFullPath), file.text)
                            }
                        }
                    }
                } catch (e: ComparisonFailure) {
                    throw e
                } catch (e: AssertionError) {
                    throw e
                } catch (e: Throwable) {
                    e.printStackTrace()
                    TestCase.fail(getTestName(true))
                }
            }
        }
    }

    protected open fun checkForUnexpectedErrors(file: KtFile) {
        DirectiveBasedActionUtils.checkForUnexpectedErrors(file)
    }

    protected open fun checkAvailableActionsAreExpected(file: PsiFile, actions: Collection<IntentionAction>) {
        DirectiveBasedActionUtils.checkAvailableActionsAreExpected(file, availableActions)
    }

    private val availableActions: List<IntentionAction>
        get() {
            myFixture.doHighlighting()
            return myFixture.availableIntentions
        }

    class TestFile internal constructor(val path: String, val content: String)

    companion object {
        private fun getActionsTexts(availableActions: List<IntentionAction>): List<String> =
            availableActions.map(IntentionAction::getText)

        private fun extraFileNamePrefix(mainFileName: String): String =
            mainFileName.replace(".Main.kt", ".").replace(".Main.java", ".")

        protected fun guessFileType(file: TestFile): FileType = when {
            file.path.contains("." + KotlinFileType.EXTENSION) -> KotlinFileType.INSTANCE
            file.path.contains("." + JavaFileType.DEFAULT_EXTENSION) -> JavaFileType.INSTANCE
            else -> PlainTextFileType.INSTANCE
        }

        private fun findActionByPattern(pattern: Pattern, availableActions: List<IntentionAction>): IntentionAction? =
            availableActions.firstOrNull { pattern.matcher(it.text).matches() }

        fun doAction(
            text: String,
            file: PsiFile,
            editor: Editor,
            actionShouldBeAvailable: Boolean,
            testFilePath: String,
            getAvailableActions: () -> List<IntentionAction>,
            doHighlighting: () -> List<HighlightInfo>,
            shouldBeAvailableAfterExecution: Boolean = false,
            checkAvailableActionsAreExpected: (PsiFile, Collection<IntentionAction>) -> Unit =
                DirectiveBasedActionUtils::checkAvailableActionsAreExpected
        ) {
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
                    TestCase.fail(
                        "Action with text '" + text + "' is not available in test " + testFilePath + "\n" +
                                "Available actions (" + texts.size + "): \n" +
                                StringUtil.join(texts, "\n") +
                                "\nActions:\n" +
                                StringUtil.join(availableActions, "\n") +
                                "\nInfos:\n" +
                                StringUtil.join(infos, "\n")
                    )
                } else {
                    checkAvailableActionsAreExpected(file, availableActions)
                }
            } else {
                if (!actionShouldBeAvailable) {
                    TestCase.fail("Action '$text' is available (but must not) in test $testFilePath")
                }

                CodeInsightTestFixtureImpl.invokeIntention(action, file, editor)

                if (!shouldBeAvailableAfterExecution) {
                    val afterAction = findActionByPattern(pattern, getAvailableActions())

                    if (afterAction != null) {
                        TestCase.fail("Action '$text' is still available after its invocation in test $testFilePath")
                    }
                }
            }
        }
    }
}
