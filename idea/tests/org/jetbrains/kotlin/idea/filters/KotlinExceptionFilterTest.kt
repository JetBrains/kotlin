/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.filters

import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.refactoring.toVirtualFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.junit.Assert
import java.io.File

private data class SuffixOption(val suffix: String, val expectedLine: Int, val expectedColumn: Int)

class KotlinExceptionFilterTest : KotlinLightCodeInsightFixtureTestCase() {
    private val mySuffixPlug = "<suffix>"
    private val myPathPlug = "<path>"

    private val myTemplates = listOf(
        "\tat 1   main.kexe\t\t 0x000000010d7cdb4c kfun:package.function(kotlin.Int) + 108 ($myPathPlug:$mySuffixPlug)\n"
    )

    private var myFiles = HashMap<String, VirtualFile>()
    private var myExceptionLine: String = ""

    private val mySuffixOptions = listOf(
        SuffixOption("10:1", 10, 1),
        SuffixOption("14:11", 14, 11),
        SuffixOption("<unknown>", 0, 0)
    )

    private fun errorMessage(detail: String) = "Failed to parse Kotlin Native exception '$myExceptionLine': $detail"

    override fun getProjectDescriptor() = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR

    override fun getTestDataPath() = ""

    override fun setUp() {
        super.setUp()
        val rootDir = File("idea/testData/debugger/nativeExceptions/")
        rootDir.listFiles().forEach {
            val virtualFile = it.toVirtualFile()
            if (virtualFile != null) {
                myFiles[it.absolutePath] = virtualFile
            }
        }
    }

    fun testDifferentLocations() {
        for (template in myTemplates) {
            for (suffixOption in mySuffixOptions) {
                val templateWithSuffix = template.replace(mySuffixPlug, suffixOption.suffix)
                doTest(templateWithSuffix, suffixOption.expectedLine, suffixOption.expectedColumn)
            }
        }
    }

    fun doTest(template: String, expectedLine: Int, expectedColumn: Int) {
        val filter = KotlinExceptionFilterFactory().create(GlobalSearchScope.allScope(project))

        for ((absolutePath, virtualFile) in myFiles) {
            myExceptionLine = template.replace(myPathPlug, absolutePath)

            val filterResult = filter.applyFilter(myExceptionLine, myExceptionLine.length)
            Assert.assertNotNull(errorMessage("filename is not found by parser"), filterResult)
            val fileHyperlinkInfo = filterResult?.firstHyperlinkInfo as FileHyperlinkInfo

            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            Assert.assertNotNull(errorMessage("test file $absolutePath could not be found in repository"), document)
            val expectedOffset = document!!.getLineStartOffset(expectedLine) + expectedColumn

            val descriptor = fileHyperlinkInfo.descriptor
            Assert.assertNotNull(errorMessage("found file hyperlink with null descriptor"), descriptor)
            Assert.assertEquals(errorMessage("different filename parsed"), virtualFile.canonicalPath, descriptor?.file?.canonicalPath)
            Assert.assertEquals(errorMessage("different offset parsed"), expectedOffset, descriptor?.offset)
        }
    }
}