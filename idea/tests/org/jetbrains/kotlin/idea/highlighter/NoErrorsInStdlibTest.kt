/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class NoErrorsInStdlibTest : KotlinLightCodeInsightFixtureTestCase() {
    fun testNoErrors() {
        val relativePathToProjectRoot = File(System.getProperty("user.dir")).relativeTo(File(myFixture.testDataPath)).path
        val root = myFixture.copyDirectoryToProject("$relativePathToProjectRoot/libraries/stdlib/src", "")

        val psiManager = psiManager // workaround for KT-3974 IllegalAccessError when accessing protected method inherited by outer class

        var totalErrors = 0
        var hasAtLeastOneFile = false

        VfsUtil.processFileRecursivelyWithoutIgnored(root) { file ->
            if (!file!!.isDirectory) {
                val psiFile = psiManager.findFile(file)
                if (psiFile is KtFile) {
                    hasAtLeastOneFile = true
                    val bindingContext = psiFile.analyzeWithContent()
                    val errors = bindingContext.diagnostics.all().filter { it.severity == Severity.ERROR }

                    if (errors.isNotEmpty()) {
                        System.err.println("${psiFile.getName()}: ${errors.size} errors")
                        AnalyzerWithCompilerReport.reportDiagnostics(
                                bindingContext.diagnostics, PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
                        )

                        totalErrors += errors.size
                    }
                }
            }

            true
        }

        assertEquals(0, totalErrors)
        assertTrue(hasAtLeastOneFile)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        // TODO: replace hardcoded path with something flexible
        return object : KotlinJdkAndLibraryProjectDescriptor(File("dist/kotlinc/lib/kotlin-stdlib.jar")) {
            override fun getSdk(): Sdk? = PluginTestCaseBase.fullJdk()
        }
    }
}
