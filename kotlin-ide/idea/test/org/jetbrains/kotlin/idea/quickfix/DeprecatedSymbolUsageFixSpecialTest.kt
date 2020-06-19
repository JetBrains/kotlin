/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
import org.jetbrains.kotlin.idea.quickfix.replaceWith.ReplaceWith
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@TestMetadata("idea/testData/quickfix.special/deprecatedSymbolUsage")
@TestDataPath("\$PROJECT_ROOT")
class DeprecatedSymbolUsageFixSpecialTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE

    fun testMemberInCompiledClass() {
        doTest("this.matches(input)")
    }

    fun testDefaultParameterValuesFromLibrary() {
        doTest("""prefix + joinTo(StringBuilder(), separator, "", postfix, limit, truncated, transform)""")
    }

    private fun doTest(pattern: String) {
        val testPath = getTestName(true) + ".kt"
        myFixture.configureByFile(testPath)

        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset)
        val nameExpression = element!!.parents.firstIsInstance<KtSimpleNameExpression>()
        project.executeWriteCommand("") {
            DeprecatedSymbolUsageFix(nameExpression, ReplaceWith(pattern, emptyList(), false)).invoke(project, editor, file)
        }

        myFixture.checkResultByFile("$testPath.after")
    }
}
