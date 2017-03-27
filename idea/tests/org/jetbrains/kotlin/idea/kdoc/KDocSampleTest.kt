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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest.wrapToFileComparisonFailure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

class KDocSampleTest : AbstractMultiModuleTest() {

    override val testPath: String = "${super.testPath}/kdoc/multiModuleSamples/"
    override fun getTestDataPath() = testPath

    override fun setUp() {
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())
        super.setUp()
    }

    fun testSimple() {

        val code = module("code")
        val samples = module("samples", hasTestRoot = true)

        samples.addDependency(code)

        doTest("simple/code/usage.kt")
    }

    fun doTest(path: String) {
        val testDataFile = File(testPath, path)
        configureByFile(path)
        val documentationManager = DocumentationManager.getInstance(myProject)
        val targetElement = documentationManager.findTargetElement(myEditor, file)
        val originalElement = DocumentationManager.getOriginalElement(targetElement)

        var info = DocumentationManager.getProviderFromElement(targetElement).generateDoc(targetElement, originalElement)
        if (info != null) {
            info = StringUtil.convertLineSeparators(info)
        }
        if (info != null && !info.endsWith("\n")) {
            info += "\n"
        }

        val textData = FileUtil.loadFile(testDataFile, true)
        val directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(textData, false, "INFO:")

        if (directives.isEmpty()) {
            throw FileComparisonFailure(
                    "'// INFO:' directive was expected",
                    textData,
                    textData + "\n\n//INFO: " + info,
                    testDataFile.absolutePath)
        }
        else {
            val expectedInfo = directives.joinToString("\n", postfix = "\n")

            if (expectedInfo.endsWith("...\n")) {
                if (!info!!.startsWith(expectedInfo.removeSuffix("...\n"))) {
                    wrapToFileComparisonFailure(info, testDataPath, textData)
                }
            }
            else if (expectedInfo != info) {
                wrapToFileComparisonFailure(info, path, textData)
            }
        }
    }
}