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

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.quickfix.utils.findInspectionFile
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

abstract class AbstractQuickFixMultiFileTest : KotlinLightCodeInsightFixtureTestCase() {

    @Throws(Exception::class)
    protected open fun doTestWithoutExtraFile(beforeFileName: String) {
        doTest(beforeFileName, false)
    }

    @Throws(Exception::class)
    protected open fun doTestWithExtraFile(beforeFileName: String) {
        enableInspections(beforeFileName)

        if (beforeFileName.endsWith(".test")) {
            doMultiFileTest(beforeFileName)
        }
        else {
            doTest(beforeFileName, true)
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun enableInspections(beforeFileName: String) {
        val inspectionFile = findInspectionFile(File(beforeFileName).parentFile)
        if (inspectionFile != null) {
            val className = FileUtil.loadFile(inspectionFile).trim { it <= ' ' }
            val inspectionClass = Class.forName(className)
            enableInspectionTools(inspectionClass)
        }
    }

    private fun enableInspectionTools(klass: Class<*>) {
        val eps = ContainerUtil.newArrayList<InspectionEP>()
        ContainerUtil.addAll<InspectionEP, LocalInspectionEP, List<InspectionEP>>(eps, *Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION))
        ContainerUtil.addAll<InspectionEP, InspectionEP, List<InspectionEP>>(eps, *Extensions.getExtensions(InspectionEP.GLOBAL_INSPECTION))

        var tool: InspectionProfileEntry? = null
        for (ep in eps) {
            if (klass.name == ep.implementationClass) {
                tool = ep.instantiateTool()
            }
        }
        assert(tool != null) { "Could not find inspection tool for class: " + klass }

        myFixture.enableInspections(tool!!)
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
    protected fun configureMultiFileTest(subFiles: List<TestFile>, beforeFile: TestFile): List<VirtualFile> {
        val vFiles = mutableListOf<VirtualFile>()
        for (subFile in subFiles) {
            vFiles.add(createTestFile(subFile))
        }
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
            vFile.setCharset(CharsetToolkit.UTF8_CHARSET)
            VfsUtil.saveText(vFile, testFile.content)
            vFile
        }
    }

    @Throws(Exception::class)
    protected fun doMultiFileTest(beforeFileName: String) {
        val multifileText = FileUtil.loadFile(File(beforeFileName), true)

        val subFiles = KotlinTestUtils.createTestFiles(
                "single.kt",
                multifileText,
                object : KotlinTestUtils.TestFileFactoryNoModules<TestFile>() {
                    override fun create(fileName: String, text: String, directives: Map<String, String>): TestFile {
                        var text = text
                        if (text.startsWith("// FILE")) {
                            val firstLineDropped = StringUtil.substringAfter(text, "\n")!!

                            text = firstLineDropped
                        }
                        return TestFile(fileName, text)
                    }
                })

        val afterFile = subFiles.firstOrNull { file -> file.path.contains(".after") }
        val beforeFile = subFiles.firstOrNull { file -> file.path.contains(".before") }!!

        subFiles.remove(beforeFile)
        if (afterFile != null) {
            subFiles.remove(afterFile)
        }

        configureMultiFileTest(subFiles, beforeFile)

        CommandProcessor.getInstance().executeCommand(project, {
            try {
                val psiFile = file

                val actionHint = ActionHint.parse(psiFile, beforeFile.content)
                val text = actionHint.expectedText

                val actionShouldBeAvailable = actionHint.shouldPresent()

                if (psiFile is KtFile) {
                    DirectiveBasedActionUtils.checkForUnexpectedErrors(psiFile)
                }

                doAction(text, actionShouldBeAvailable, getTestName(false))

                val actualText = file.text
                val afterText = StringBuilder(actualText).insert(editor.caretModel.offset, "<caret>").toString()

                if (actionShouldBeAvailable) {
                    TestCase.assertNotNull(".after file should exist", afterFile)
                    if (afterText != afterFile!!.content) {
                        val actualTestFile = StringBuilder()
                        actualTestFile.append("// FILE: ").append(beforeFile.path).append("\n").append(beforeFile.content)
                        for (file in subFiles) {
                            actualTestFile.append("// FILE: ").append(file.path).append("\n").append(file.content)
                        }
                        actualTestFile.append("// FILE: ").append(afterFile.path).append("\n").append(afterText)

                        KotlinTestUtils.assertEqualsToFile(File(beforeFileName), actualTestFile.toString())
                    }
                }
                else {
                    TestCase.assertNull(".after file should not exist", afterFile)
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

    @Throws(Exception::class)
    private fun doTest(beforeFileName: String, withExtraFile: Boolean) {
        val mainFile = File(beforeFileName)
        val originalFileText = FileUtil.loadFile(mainFile, true)

        if (withExtraFile) {
            val mainFileDir = mainFile.parentFile!!

            val mainFileName = mainFile.name
            val extraFiles = mainFileDir.listFiles { dir, name -> name.startsWith(extraFileNamePrefix(mainFileName)) && name != mainFileName }!!

            val testFiles = ArrayList<String>()
            testFiles.add(mainFile.name)
            extraFiles.mapTo(testFiles) { file -> file.name }

            myFixture.configureByFiles(*testFiles.toTypedArray())
        }
        else {
            myFixture.configureByFile(beforeFileName)
        }

        CommandProcessor.getInstance().executeCommand(project, {
            try {
                val psiFile = file

                val actionHint = ActionHint.parse(psiFile, originalFileText)
                val text = actionHint.expectedText

                val actionShouldBeAvailable = actionHint.shouldPresent()

                if (psiFile is KtFile) {
                    DirectiveBasedActionUtils.checkForUnexpectedErrors(psiFile)
                }

                doAction(text, actionShouldBeAvailable, beforeFileName)

                if (actionShouldBeAvailable) {
                    val afterFilePath = beforeFileName.replace(".before.Main.", ".after.")
                    try {
                        myFixture.checkResultByFile(mainFile.name.replace(".before.Main.", ".after."))
                    }
                    catch (e: ComparisonFailure) {
                        KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
                    }

                    val mainFile = myFixture.file
                    val mainFileName = mainFile.name
                    for (file in mainFile.containingDirectory.files) {
                        val fileName = file.name
                        if (fileName == mainFileName || !fileName.startsWith(extraFileNamePrefix(myFixture.file.name))) continue

                        val extraFileFullPath = beforeFileName.replace(mainFileName, fileName)
                        val afterFile = File(extraFileFullPath.replace(".before.", ".after."))
                        if (afterFile.exists()) {
                            KotlinTestUtils.assertEqualsToFile(afterFile, file.text)
                        }
                        else {
                            KotlinTestUtils.assertEqualsToFile(File(extraFileFullPath), file.text)
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

    @Throws(Exception::class)
    fun doAction(text: String, actionShouldBeAvailable: Boolean, testFilePath: String) {
        val pattern = if (text.startsWith("/"))
            Pattern.compile(text.substring(1, text.length - 1))
        else
            Pattern.compile(StringUtil.escapeToRegexp(text))

        val availableActions = availableActions
        val action = findActionByPattern(pattern, availableActions)

        if (action == null) {
            if (actionShouldBeAvailable) {
                val texts = getActionsTexts(availableActions)
                val infos = myFixture.doHighlighting()
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

            CodeInsightTestFixtureImpl.invokeIntention(action, file, editor, action.text)

            if (!shouldBeAvailableAfterExecution()) {
                val afterAction = findActionByPattern(pattern, this.availableActions)

                if (afterAction != null) {
                    TestCase.fail("Action '$text' is still available after its invocation in test $testFilePath")
                }
            }
        }
    }


    private val availableActions: List<IntentionAction>
        get() {
            myFixture.doHighlighting()
            return myFixture.availableIntentions
        }

    class TestFile internal constructor(val path: String, val content: String)

    companion object {

        protected fun shouldBeAvailableAfterExecution(): Boolean {
            return false
        }

        private fun getActionsTexts(availableActions: List<IntentionAction>): List<String> {
            val texts = ArrayList<String>()
            for (intentionAction in availableActions) {
                texts.add(intentionAction.text)
            }
            return texts
        }

        private fun extraFileNamePrefix(mainFileName: String): String {
            return mainFileName.replace(".Main.kt", ".").replace(".Main.java", ".")
        }

        protected fun guessFileType(file: TestFile): FileType {
            if (file.path.contains("." + KotlinFileType.EXTENSION)) {
                return KotlinFileType.INSTANCE
            }
            else if (file.path.contains("." + JavaFileType.DEFAULT_EXTENSION)) {
                return JavaFileType.INSTANCE
            }
            else {
                return PlainTextFileType.INSTANCE
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
    }
}
