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

package org.jetbrains.kotlin.idea.inspections

import com.google.common.collect.Lists
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.testFramework.InspectionTestUtil
import com.intellij.testFramework.createGlobalContextForTool
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractLocalInspectionTest  : KotlinLightCodeInsightFixtureTestCase() {
    protected open fun inspectionFileName(): String = ".inspection"

    protected open fun afterFileNameSuffix(): String = ".after"

    protected open fun expectedProblemDirectiveName(): String = "PROBLEM"

    protected open fun fixTextDirectiveName(): String = "FIX"

    private fun createInspection(testDataFile: File): AbstractKotlinInspection {
        val candidateFiles = Lists.newArrayList<File>()

        var current: File? = testDataFile.parentFile
        while (current != null) {
            val candidate = File(current, inspectionFileName())
            if (candidate.exists()) {
                candidateFiles.add(candidate)
            }
            current = current.parentFile
        }

        if (candidateFiles.isEmpty()) {
            throw AssertionError(".inspection file is not found for " + testDataFile +
                                 "\nAdd it to base directory of test data. It should contain fully-qualified name of inspection class.")
        }
        if (candidateFiles.size > 1) {
            throw AssertionError("Several .inspection files are available for " + testDataFile +
                                 "\nPlease remove some of them\n" + candidateFiles)
        }

        val className = FileUtil.loadFile(candidateFiles[0]).trim { it <= ' ' }
        return Class.forName(className).newInstance() as AbstractKotlinInspection
    }

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val inspection = createInspection(mainFile)

        val psiFile = myFixture.configureByFiles(mainFile.name).first()

        val fileText = FileUtil.loadFile(mainFile, true)
        TestCase.assertTrue("\"<caret>\" is missing in file \"$mainFile\"", fileText.contains("<caret>"))

        val minJavaVersion = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MIN_JAVA_VERSION: ")
        if (minJavaVersion != null && !SystemInfo.isJavaVersionAtLeast(minJavaVersion)) return

        if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_BEFORE")) {
            DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
        }

        doTestFor(mainFile.name, psiFile, inspection, fileText)

        if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_AFTER")) {
            DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
        }
    }

    private fun doTestFor(mainFilePath: String, psiFile: PsiFile, inspection: AbstractKotlinInspection, fileText: String) {
        val problemExpectedString = InTextDirectivesUtils.findStringWithPrefixes(
                fileText, "// ${expectedProblemDirectiveName()}: ")
        val problemExpected = problemExpectedString == null || problemExpectedString != "none"

        val toolWrapper = LocalInspectionToolWrapper(inspection)
        val scope = AnalysisScope(project, listOf(psiFile.virtualFile!!))
        scope.invalidate()
        val globalContext = createGlobalContextForTool(scope, project, listOf(toolWrapper))
        InspectionTestUtil.runTool(toolWrapper, scope, globalContext)

        val problemDescriptors = globalContext.getPresentation(toolWrapper).problemDescriptors
                .filterIsInstance<ProblemDescriptor>()
                .filter { editor.caretModel.offset in it.psiElement.textRange }
        Assert.assertTrue(
                if (!problemExpected)
                    "No problems should be detected at caret\n" +
                    "Detected problems: ${problemDescriptors.joinToString { it.descriptionTemplate }}"
                else
                    "Expected at least one problem at caret",
                problemExpected == problemDescriptors.isNotEmpty())
        if (!problemExpected) return
        if (problemExpectedString != null) {
            Assert.assertTrue("Expected the following problem at caret: $problemExpectedString\n" +
                              "Active problems: ${problemDescriptors.joinToString { it.descriptionTemplate }}",
                              problemDescriptors.any { it.descriptionTemplate == problemExpectedString })
        }

        val localFixTextString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// " + fixTextDirectiveName() + ": ")
        val localFixActions = problemDescriptors.flatMap {
            problem ->
            val fixes = problem.fixes
            fixes?.toList() ?: emptyList()
        }.filter { fix -> localFixTextString == null || fix.name == localFixTextString }

        val availableDescription = localFixActions.joinToString { it.name }

        val fixDescription = localFixTextString?.let { "with specified text '$localFixTextString'"} ?: ""
        TestCase.assertTrue("No fix action $fixDescription\n" +
                            "Available actions: $availableDescription",
                            localFixActions.isNotEmpty())

        val localFixAction = localFixActions.singleOrNull()
        TestCase.assertTrue("More than one fix action $fixDescription\n" +
                            "Available actions: $availableDescription",
                            localFixAction != null)

        val problemDescriptor = problemDescriptors.find { localFixAction in it.fixes?.toList() ?: emptyList() }!!

        project.executeWriteCommand(localFixAction!!.name, null) {
            localFixAction.applyFix(project, problemDescriptor)
            null
        }
        val canonicalPathToExpectedFile = mainFilePath + afterFileNameSuffix()
        try {
            myFixture.checkResultByFile(canonicalPathToExpectedFile)
        }
        catch (e: ComparisonFailure) {
            KotlinTestUtils.assertEqualsToFile(
                    File(testDataPath, canonicalPathToExpectedFile),
                    editor.document.text
            )
        }
    }

}