/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinCleanupInspectionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/inspections/cleanup"

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private fun doTest(dir: String, result: String, vararg files: String) {
        myFixture.enableInspections(KotlinCleanupInspection::class.java)
        myFixture.enableInspections(SortModifiersInspection::class.java)
        myFixture.enableInspections(RedundantModalityModifierInspection::class.java)
        myFixture.configureByFiles(*files.map { "$dir/$it" }.toTypedArray())

        val project = myFixture.project
        val managerEx = InspectionManager.getInstance(project)
        val globalContext = managerEx.createNewGlobalContext(false) as GlobalInspectionContextBase
        val analysisScope = AnalysisScope(myFixture.file)
        val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
        globalContext.codeCleanup(analysisScope, profile, "Cleanup", null, true)

        myFixture.checkResultByFile("$dir/$result")
    }

    fun testBasic() {
        doTest("basic", "basic.kt.after", "basic.kt", "JavaAnn.java", "deprecatedSymbols.kt")
    }

    fun testFileWithAnnotationToSuppressDeprecation() {
        doTest(
            "fileWithAnnotationToSuppressDeprecation",
            "fileWithAnnotationToSuppressDeprecation.kt.after",
            "fileWithAnnotationToSuppressDeprecation.kt",
            "deprecatedSymbols.kt"
        )
    }
}
