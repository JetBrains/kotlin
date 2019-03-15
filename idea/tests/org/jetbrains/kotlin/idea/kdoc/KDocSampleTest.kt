/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest.wrapToFileComparisonFailure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

class KDocSampleTest : AbstractMultiModuleTest() {

    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/kdoc/multiModuleSamples/"

    fun testSimple() {

        val code = module("code")
        val samples = module("samples", hasTestRoot = true)

        samples.addDependency(code)

        doInfoTest("simple/code/usage.kt")
    }

    fun testFqName() {

        val code = module("code")
        val samples = module("samples", hasTestRoot = true)

        samples.addDependency(code)

        doInfoTest("fqName/code/usage.kt")
        doResolveTest("fqName/code/usage.kt", "samples")
        doResolveTest("fqName/code/usage.kt", "samples.SampleGroup")
        doResolveTest("fqName/code/usage.kt", "samples.megasamples")
        doResolveTest("fqName/code/usage.kt", "samples.megasamples.MegaSamplesGroup")
        doResolveTest("fqName/code/usage.kt", "samples.notindir")
        doResolveTest("fqName/code/usage.kt", "samples.notindir.NotInDirSamples")
        doResolveTest("fqName/code/usage.kt", "samplez")
        doResolveTest("fqName/code/usage.kt", "samplez.a")
        doResolveTest("fqName/code/usage.kt", "samplez.a.b")
        doResolveTest("fqName/code/usage.kt", "samplez.a.b.c")
        doResolveTest("fqName/code/usage.kt", "samplez.a.b.c.Samplez")
    }

    fun doResolveTest(path: String, link: String) {

        configureByFile(path)
        val documentationManager = DocumentationManager.getInstance(myProject)
        val targetElement = documentationManager.findTargetElement(myEditor, file)

        targetElement as KtElement

        val bindingContext = targetElement.analyze()
        val descriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, targetElement]!!
        val kdoc = descriptor.findKDoc()!! as KDocSection
        val resolutionFacade = targetElement.getResolutionFacade()
        assertNotEmpty(resolveKDocLink(bindingContext, resolutionFacade, descriptor, kdoc.findTagByName("sample")!!, link.split(".")))

    }

    fun doInfoTest(path: String) {
        val testDataFile = File(testDataPath, path)
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
                    wrapToFileComparisonFailure(info, testDataFile.absolutePath, textData)
                }
            }
            else if (expectedInfo != info) {
                wrapToFileComparisonFailure(info!!, testDataFile.absolutePath, textData)
            }
        }
    }
}