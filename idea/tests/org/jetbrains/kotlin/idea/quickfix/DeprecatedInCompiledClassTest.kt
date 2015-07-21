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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.runner.RunWith

TestMetadata("idea/testData/quickfix.special")
TestDataPath("\$PROJECT_ROOT")
RunWith(JUnit3RunnerWithInners::class)
public class QuickFixSpecialTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestUtils.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    TestMetadata("deprecatedMemberInCompiledClass.kt")
    public fun testDeprecatedMemberInCompiledClass() {
        val testPath = JetTestUtils.navigationMetadata("idea/testData/quickfix.special/deprecatedMemberInCompiledClass.kt")
        myFixture.configureByFile(testPath)

        val offset = getEditor().getCaretModel().getOffset()
        val element = getFile().findElementAt(offset)
        val nameExpression = element!!.parents.firstIsInstance<JetSimpleNameExpression>()
        getProject().executeWriteCommand("") {
            DeprecatedSymbolUsageFix(nameExpression, ReplaceWith("matches(input)")).invoke(getProject(), getEditor(), getFile())
        }

        myFixture.checkResultByFile("$testPath.after")
    }
}
