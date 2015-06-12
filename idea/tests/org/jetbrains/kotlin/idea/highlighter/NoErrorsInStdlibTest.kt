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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.test.JetJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.JetFile
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class NoErrorsInStdlibTest : JetLightCodeInsightFixtureTestCase() {
    public fun testNoErrors() {
        val root = myFixture.copyDirectoryToProject("../libraries/stdlib/src", "")

        val psiManager = getPsiManager() // workaround for KT-3974 IllegalAccessError when accessing protected method inherited by outer class

        var totalErrors = 0
        var hasAtLeastOneFile = false

        VfsUtil.processFileRecursivelyWithoutIgnored(root) { file ->
            if (!file!!.isDirectory()) {
                val psiFile = psiManager.findFile(file)
                if (psiFile is JetFile) {
                    hasAtLeastOneFile = true
                    val bindingContext = psiFile.analyzeFully()
                    val errors = bindingContext.getDiagnostics().all().filter { it.getSeverity() == Severity.ERROR }

                    if (errors.isNotEmpty()) {
                        System.err.println("${psiFile.getName()}: ${errors.size()} errors")
                        AnalyzerWithCompilerReport.reportDiagnostics(
                                bindingContext.getDiagnostics(), PrintingMessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR
                        )

                        totalErrors += errors.size()
                    }
                }
            }

            true
        }

        assertEquals(0, totalErrors)
        assertTrue(hasAtLeastOneFile)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : JetJdkAndLibraryProjectDescriptor(File("dist/classes/builtins")) {
            override fun getSdk(): Sdk? = PluginTestCaseBase.fullJdk()
        }
    }
}
