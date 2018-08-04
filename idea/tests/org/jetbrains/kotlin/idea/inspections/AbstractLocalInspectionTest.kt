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
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.intention.EmptyIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import junit.framework.ComparisonFailure
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.configureCompilerOptions
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractLocalInspectionTest : KotlinLightCodeInsightFixtureTestCase() {
    private val inspectionFileName: String
        get() = ".inspection"

    private val afterFileNameSuffix: String
        get() = ".after"

    private val expectedProblemDirectiveName: String
        get() = "PROBLEM"

    private val expectedProblemHighlightType: String
        get() = "HIGHLIGHT"

    private val fixTextDirectiveName: String
        get() = "FIX"

    private fun createInspection(testDataFile: File): AbstractKotlinInspection {
        val candidateFiles = Lists.newArrayList<File>()

        var current: File? = testDataFile.parentFile
        while (current != null) {
            val candidate = File(current, inspectionFileName)
            if (candidate.exists()) {
                candidateFiles.add(candidate)
            }
            current = current.parentFile
        }

        if (candidateFiles.isEmpty()) {
            throw AssertionError(
                ".inspection file is not found for " + testDataFile +
                        "\nAdd it to base directory of test data. It should contain fully-qualified name of inspection class."
            )
        }
        if (candidateFiles.size > 1) {
            throw AssertionError(
                "Several .inspection files are available for " + testDataFile +
                        "\nPlease remove some of them\n" + candidateFiles
            )
        }

        val className = FileUtil.loadFile(candidateFiles[0]).trim { it <= ' ' }
        return Class.forName(className).newInstance() as AbstractKotlinInspection
    }

    protected open fun doTest(path: String) {
        val mainFile = File(path)
        val inspection = createInspection(mainFile)

        val fileText = FileUtil.loadFile(mainFile, true)
        TestCase.assertTrue("\"<caret>\" is missing in file \"$mainFile\"", fileText.contains("<caret>"))

        configureCompilerOptions(fileText, project, module)

        val minJavaVersion = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MIN_JAVA_VERSION: ")
        if (minJavaVersion != null && !SystemInfo.isJavaVersionAtLeast(minJavaVersion)) return

        if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_BEFORE")) {
            DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
        }

        var i = 1
        val extraFileNames = mutableListOf<String>()
        extraFileLoop@ while (true) {
            for (extension in EXTENSIONS) {
                val extraFile = File(mainFile.parent, FileUtil.getNameWithoutExtension(mainFile) + "." + i + extension)
                if (extraFile.exists()) {
                    extraFileNames += extraFile.name
                    i++
                    continue@extraFileLoop
                }
            }
            break
        }

        myFixture.configureByFiles(*(listOf(mainFile.name) + extraFileNames).toTypedArray()).first()

        doTestFor(mainFile.name, inspection, fileText)

        if (file is KtFile && !InTextDirectivesUtils.isDirectiveDefined(fileText, "// SKIP_ERRORS_AFTER")) {
            DirectiveBasedActionUtils.checkForUnexpectedErrors(file as KtFile)
        }
    }

    protected fun runInspectionWithFixesAndCheck(
        inspection: AbstractKotlinInspection,
        expectedProblemString: String?,
        expectedHighlightString: String?,
        localFixTextString: String?
    ): Boolean {
        val problemExpected = expectedProblemString == null || expectedProblemString != "none"
        myFixture.enableInspections(inspection::class.java)

        // Set default level to WARNING to make possible to test DO_NOT_SHOW
        val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)
        val inspectionProfile = inspectionProfileManager.currentProfile
        val state = inspectionProfile.getToolDefaultState(inspection.shortName, project)
        state.level = HighlightDisplayLevel.WARNING

        val caretOffset = myFixture.caretOffset
        val highlightInfos = CodeInsightTestFixtureImpl.instantiateAndRun(
            file, editor, intArrayOf(
                Pass.LINE_MARKERS,
                Pass.EXTERNAL_TOOLS,
                Pass.POPUP_HINTS,
                Pass.UPDATE_ALL,
                Pass.UPDATE_FOLDING,
                Pass.WOLF
            ), false
        ).filter { it.description != null && caretOffset in it.startOffset..it.endOffset }

        Assert.assertTrue(
            if (!problemExpected)
                "No problems should be detected at caret\n" +
                        "Detected problems: ${highlightInfos.joinToString { it.description }}"
            else
                "Expected at least one problem at caret",
            problemExpected == highlightInfos.isNotEmpty()
        )
        if (!problemExpected || highlightInfos.isEmpty()) return false
        highlightInfos
            .filter { it.type != HighlightInfoType.INFORMATION }
            .forEach {
                val description = it.description
                Assert.assertTrue(
                    "Problem description should not contain 'can': $description",
                    " can " !in description
                )
            }

        if (expectedProblemString != null) {
            Assert.assertTrue(
                "Expected the following problem at caret: $expectedProblemString\n" +
                        "Active problems: ${highlightInfos.joinToString { it.description }}",
                highlightInfos.any { it.description == expectedProblemString }
            )
        }
        val expectedHighlightType = when (expectedHighlightString) {
            null -> null
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING.name -> HighlightDisplayLevel.WARNING.name
            else -> expectedHighlightString
        }
        if (expectedHighlightType != null) {
            Assert.assertTrue(
                "Expected the following problem highlight type: $expectedHighlightType\n" +
                        "Actual type: ${highlightInfos.joinToString { it.type.toString() }}",
                highlightInfos.all { expectedHighlightType in it.type.toString() }
            )
        }

        val allLocalFixActions = highlightInfos.flatMap { it.quickFixActionMarkers ?: emptyList() }.map { it.first.action }

        val localFixActions = if (localFixTextString == null || localFixTextString == "none") {
            allLocalFixActions
        } else {
            allLocalFixActions.filter { fix -> fix.text == localFixTextString }
        }

        val availableDescription = allLocalFixActions.joinToString { it.text }

        val fixDescription = localFixTextString?.let { "with specified text '$localFixTextString'" } ?: ""
        TestCase.assertTrue(
            "No fix action $fixDescription\n" +
                    "Available actions: $availableDescription",
            localFixActions.isNotEmpty()
        )

        val localFixAction = localFixActions.singleOrNull { it !is EmptyIntentionAction }
        if (localFixTextString == "none") {
            Assert.assertTrue("Expected no fix action", localFixAction == null)
            return false
        }
        TestCase.assertTrue(
            "More than one fix action $fixDescription\n" +
                    "Available actions: $availableDescription",
            localFixAction != null
        )

        project.executeWriteCommand(localFixAction!!.text, null) {
            localFixAction.invoke(project, editor, file)
        }
        return true
    }

    private fun doTestFor(mainFilePath: String, inspection: AbstractKotlinInspection, fileText: String) {
        val expectedProblemString = InTextDirectivesUtils.findStringWithPrefixes(
            fileText, "// $expectedProblemDirectiveName: "
        )
        val expectedHighlightString = InTextDirectivesUtils.findStringWithPrefixes(
            fileText, "// $expectedProblemHighlightType: "
        )
        val localFixTextString = InTextDirectivesUtils.findStringWithPrefixes(
            fileText, "// $fixTextDirectiveName: "
        )

        if (!runInspectionWithFixesAndCheck(inspection, expectedProblemString, expectedHighlightString, localFixTextString)) {
            return
        }

        val canonicalPathToExpectedFile = mainFilePath + afterFileNameSuffix
        try {
            myFixture.checkResultByFile(canonicalPathToExpectedFile)
        } catch (e: ComparisonFailure) {
            KotlinTestUtils.assertEqualsToFile(
                File(testDataPath, canonicalPathToExpectedFile),
                editor.document.text
            )
        }
    }

    companion object {
        private val EXTENSIONS = arrayOf(".kt", ".kts", ".java", ".groovy")
    }
}