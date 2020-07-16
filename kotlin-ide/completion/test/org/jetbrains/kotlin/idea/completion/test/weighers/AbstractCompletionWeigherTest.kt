/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.weighers

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.withCustomCompilerOptions
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.Assert
import java.io.File

abstract class AbstractCompletionWeigherTest(val completionType: CompletionType, val relativeTestDataPath: String) :
    KotlinLightCodeInsightFixtureTestCase() {
    fun doTest(path: String) {
        myFixture.configureWithExtraFile(
            File(path).toRelativeString(File(testDataPath)),
            ".Data", ".Data1", ".Data2", ".Data3", ".Data4", ".Data5", ".Data6"
        )

        val text = myFixture.editor.document.text

        val items = InTextDirectivesUtils.findArrayWithPrefixes(text, "// ORDER:")
        Assert.assertTrue("""Some items should be defined with "// ORDER:" directive""", items.isNotEmpty())

        withCustomCompilerOptions(text, project, module) {
            myFixture.complete(completionType, InTextDirectivesUtils.getPrefixedInt(text, "// INVOCATION_COUNT:") ?: 1)
            myFixture.assertPreferredCompletionItems(InTextDirectivesUtils.getPrefixedInt(text, "// SELECTED:") ?: 0, *items)
        }
    }
}

abstract class AbstractBasicCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.BASIC, "weighers/basic") {
}

abstract class AbstractSmartCompletionWeigherTest() : AbstractCompletionWeigherTest(CompletionType.SMART, "weighers/smart") {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
