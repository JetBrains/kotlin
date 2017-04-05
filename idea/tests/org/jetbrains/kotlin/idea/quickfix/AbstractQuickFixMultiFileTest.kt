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
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.quickFix.ActionHint
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.codeInspection.InspectionEP
import com.intellij.codeInspection.InspectionProfileEntry
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.Result
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.VfsTestUtil
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.quickfix.utils.*
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.runWriteAction

import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

abstract class AbstractQuickFixMultiFileTest : KotlinDaemonAnalyzerTestCase() {

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

        enableInspectionTools(tool!!)
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = arrayOf("excludedPackage", "somePackage.ExcludedClass")
    }

    @Throws(Exception::class)
    override fun tearDown() {
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY
        super.tearDown()
    }

    /**
     * @param subFiles   subFiles of multiFile test
     * *
     * @param beforeFile will be added last, as subFiles are dependencies of it
     */
    protected fun configureMultiFileTest(subFiles: List<TestFile>, beforeFile: TestFile) {
        try {
            val sourceRootDir = createTempDirectory()
            val virtualFiles = HashMap<TestFile, VirtualFile>()

            for (file in subFiles) {
                virtualFiles.put(file, createVirtualFileFromTestFile(sourceRootDir, file))
            }
            virtualFiles.put(beforeFile, createVirtualFileFromTestFile(sourceRootDir, beforeFile))

            val sourceRootVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceRootDir)
            PsiTestUtil.addSourceRoot(myModule, sourceRootVFile)

            for (file in subFiles) {
                configureByExistingFile(virtualFiles[file]!!)
                TestCase.assertEquals(guessFileType(file), myFile.virtualFile.fileType)
            }

            configureByExistingFile(virtualFiles[beforeFile]!!)
            TestCase.assertEquals(guessFileType(beforeFile), myFile.virtualFile.fileType)

            TestCase.assertTrue("\"<caret>\" is probably missing in file \"" + beforeFile.path + "\"", myEditor.caretModel.offset != 0)
        }
        catch (e: IOException) {
            throw RuntimeException(e)
        }

    }


    @Throws(Exception::class)
    protected fun doMultiFileTest(beforeFileName: String) {
        val multifileText = FileUtil.loadFile(File(beforeFileName), true)

        val withRuntime = InTextDirectivesUtils.isDirectiveDefined(multifileText, "// WITH_RUNTIME")
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
        }

        try {
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
        finally {
            if (withRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk())
            }
        }
    }

    @Throws(Exception::class)
    private fun doTest(beforeFileName: String, withExtraFile: Boolean) {
        val testDataPath = testDataPath
        val mainFile = File(testDataPath + beforeFileName)
        val originalFileText = FileUtil.loadFile(mainFile, true)

        val withRuntime = InTextDirectivesUtils.isDirectiveDefined(originalFileText, "// WITH_RUNTIME")
        val fullJdk = InTextDirectivesUtils.isDirectiveDefined(originalFileText, "// FULL_JDK")
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, if (fullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk())
        }

        try {
            if (withExtraFile) {
                val mainFileDir = mainFile.parentFile!!

                val mainFileName = mainFile.name
                val extraFiles = mainFileDir.listFiles { dir, name -> name.startsWith(extraFileNamePrefix(mainFileName)) && name != mainFileName }!!

                val testFiles = ArrayList<String>()
                testFiles.add(beforeFileName)
                extraFiles.mapTo(testFiles) { file -> beforeFileName.replace(mainFileName, file.name) }

                configureByFiles(null, *ArrayUtil.toStringArray(testFiles))
            }
            else {
                configureByFiles(null, beforeFileName)
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
                            checkResultByFile(afterFilePath)
                        }
                        catch (e: ComparisonFailure) {
                            KotlinTestUtils.assertEqualsToFile(File(afterFilePath), editor)
                        }

                        val mainFile = myFile
                        val mainFileName = mainFile.name
                        for (file in mainFile.containingDirectory.files) {
                            val fileName = file.name
                            if (fileName == mainFileName || !fileName.startsWith(extraFileNamePrefix(myFile.name))) continue

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
        finally {
            if (withRuntime) {
                ConfigLibraryUtil
                        .unConfigureKotlinRuntimeAndSdk(myModule, if (fullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk())
            }
        }
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


            if (!shouldBeAvailableAfterExecution()) {
                val afterAction = findActionByPattern(pattern, availableActions)

                if (afterAction != null) {
                    TestCase.fail("Action '$text' is still available after its invocation in test $testFilePath")
                }
            }
        }
    }


    private val availableActions: List<IntentionAction>
        get() {
            doHighlighting()
            return LightQuickFixTestCase.getAvailableActions(editor, file)
        }

    override fun getTestProjectJdk(): Sdk? {
        return PluginTestCaseBase.mockJdk()
    }

    override fun getTestDataPath(): String {
        return KotlinTestUtils.getHomeDirectory() + "/"
    }

    private fun findVirtualFile(filePath: String): VirtualFile {
        val absolutePath = testDataPath + filePath
        return VfsTestUtil.findFileByCaseSensitivePath(absolutePath)
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

        /**
         * @param sourceRootDir Base path of test file(Test source directory)
         * *
         * @param testFile      source of VFile content
         * *
         * @return created VirtualFile
         */
        protected fun createVirtualFileFromTestFile(sourceRootDir: File, testFile: TestFile): VirtualFile {
            try {
                TestCase.assertFalse("Please don't use absolute path for multifile test 'FILE' directive: " + testFile.path,
                                     FileUtil.isAbsolutePlatformIndependent(testFile.path))
                val fileType = guessFileType(testFile)
                val extension = fileType.defaultExtension


                val fileInSourceRoot = File(testFile.path)
                var container = FileUtil.getParentFile(fileInSourceRoot)
                if (container == null) {
                    container = sourceRootDir
                }
                else {
                    container = File(sourceRootDir, container.path)
                }

                if (!container.exists()) {
                    TestCase.assertTrue(container.mkdirs())
                }

                val tempFile = FileUtil.createTempFile(container, FileUtil.getNameWithoutExtension(testFile.path), "." + extension, true)


                val vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile)!!
                runWriteAction {
                    vFile.setCharset(CharsetToolkit.UTF8_CHARSET)
                    VfsUtil.saveText(vFile, testFile.content)

                }
                return vFile
            }
            catch (e: IOException) {
                throw RuntimeException(e)
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
