/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Ignore
import org.junit.Test

class GradleBuildFileHighlightingTest : KotlinGradleImportingTestCase() {
    @TargetVersions("4.8+")
    @Test
    fun testKtsInJsProject() {
        val buildGradleKts = configureByFiles().findBuildGradleKtsFile()
        importProjectUsingSingeModulePerGradleProject()
        checkHighlighting(buildGradleKts)
    }

    @TargetVersions("4.8+")
    @Test
    fun testSimple() {
        val buildGradleKts = configureByFiles().findBuildGradleKtsFile()
        importProjectUsingSingeModulePerGradleProject()
        checkHighlighting(buildGradleKts)
    }

    @Ignore
    @TargetVersions("4.8+")
    @Test
    fun testComplexBuildGradleKts() {
        val buildGradleKts = configureByFiles().findBuildGradleKtsFile()
        importProjectUsingSingeModulePerGradleProject()
        checkHighlighting(buildGradleKts)
    }

    private fun List<VirtualFile>.findBuildGradleKtsFile(): VirtualFile {
        return singleOrNull { it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME }
            ?: error("Couldn't find any build.gradle.kts file")
    }

    private fun checkHighlighting(file: VirtualFile) {
        runInEdtAndWait {
            runReadAction {
                val psiFile = PsiManager.getInstance(myProject).findFile(file) as? KtFile
                    ?: error("Couldn't find psiFile for virtual file: ${file.canonicalPath}")

                ScriptConfigurationManager.updateScriptDependenciesSynchronously(psiFile)

                val bindingContext = psiFile.analyzeWithContent()
                val diagnostics = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }

                assert(diagnostics.isEmpty()) {
                    val diagnosticLines = diagnostics.joinToString("\n") { DefaultErrorMessages.render(it) }
                    "Diagnostic list should be empty:\n $diagnosticLines"
                }
            }
        }
    }

    override fun testDataDirName() = "highlighting"
}