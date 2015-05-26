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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.analysis.AnalysisScope
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

class KotlinCleanupInspectionTest(): JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String
            = PluginTestCaseBase.getTestDataPathBase() + "/inspections/cleanup"

    override fun getProjectDescriptor(): LightProjectDescriptor = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public fun testCleanup() {
        myFixture.enableInspections(javaClass<KotlinCleanupInspection>())
        myFixture.configureByFile("cleanup.kt")

        val project = myFixture.getProject()
        val managerEx = InspectionManager.getInstance(project)
        val globalContext = managerEx.createNewGlobalContext(false) as GlobalInspectionContextBase
        val analysisScope = AnalysisScope(myFixture.getFile())
        val profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile()
        globalContext.codeCleanup(project, analysisScope, profile, "Cleanup", null, true)

        myFixture.checkResultByFile("cleanup.kt.after")
    }
}