/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi

import com.intellij.injected.editor.DocumentWindowImpl
import com.intellij.injected.editor.EditorWindow
import com.intellij.openapi.util.TextRange
import com.intellij.psi.injection.Injectable
import com.intellij.testFramework.LightProjectDescriptor
import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.InjectLanguageAction
import org.intellij.plugins.intelliLang.inject.UnInjectLanguageAction
import org.intellij.plugins.intelliLang.references.FileReferenceInjector
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

abstract class AbstractInjectionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        val testName = getTestName(true)
        return when {
            testName.endsWith("WithAnnotation") -> KotlinLightProjectDescriptor.INSTANCE
            testName.endsWith("WithRuntime") -> KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
            else -> KotlinLightCodeInsightFixtureTestCaseBase.JAVA_LATEST
        }
    }

    data class ShredInfo(
            val range: TextRange,
            val hostRange: TextRange,
            val prefix: String = "",
            val suffix: String = "") {
    }

    protected fun doInjectionPresentTest(
            @Language("kotlin") text: String, @Language("Java") javaText: String? = null,
            languageId: String? = null, unInjectShouldBePresent: Boolean = true,
            shreds: List<ShredInfo>? = null) {
        if (javaText != null) {
            myFixture.configureByText("${getTestName(true)}.java", javaText.trimIndent())
        }

        myFixture.configureByText("${getTestName(true)}.kt", text.trimIndent())

        assertInjectionPresent(languageId, unInjectShouldBePresent)

        if (shreds != null) {
            val actualShreds = (editor.document as DocumentWindowImpl).shreds.map {
                ShredInfo(it.range, it.rangeInsideHost, it.prefix, it.suffix)
            }

            assertOrderedEquals(
                    actualShreds.sortedBy { it.range.startOffset },
                    shreds.sortedBy { it.range.startOffset })
        }
    }

    protected fun assertInjectionPresent(languageId: String?, unInjectShouldBePresent: Boolean) {
        TestCase.assertFalse("Injection action is available. There's probably no injection at caret place",
                             InjectLanguageAction().isAvailable(project, myFixture.editor, myFixture.file))

        if (languageId != null) {
            val injectedFile = (editor as? EditorWindow)?.injectedFile
            assertEquals("Wrong injection language", languageId, injectedFile?.language?.id)
        }

        if (unInjectShouldBePresent) {
            TestCase.assertTrue("UnInjection action is not available. There's no injection at caret place or some other troubles.",
                                UnInjectLanguageAction().isAvailable(project, myFixture.editor, myFixture.file))
        }
    }

    protected fun assertNoInjection(@Language("kotlin") text: String) {
        myFixture.configureByText("${getTestName(true)}.kt", text.trimIndent())

        TestCase.assertTrue("Injection action is not available. There's probably some injection but nothing was expected.",
                            InjectLanguageAction().isAvailable(project, myFixture.editor, myFixture.file))
    }

    protected fun doRemoveInjectionTest(@Language("kotlin") before: String, @Language("kotlin") after: String) {
        myFixture.setCaresAboutInjection(false)

        myFixture.configureByText("${getTestName(true)}.kt", before.trimIndent())

        TestCase.assertTrue(UnInjectLanguageAction().isAvailable(project, myFixture.editor, myFixture.file))
        UnInjectLanguageAction.invokeImpl(project, myFixture.editor, myFixture.file)

        myFixture.checkResult(after.trimIndent())
    }

    protected fun doFileReferenceInjectTest(@Language("kotlin") before: String, @Language("kotlin") after: String) {
        doTest(FileReferenceInjector(), before, after)
    }

    protected fun doTest(injectable: Injectable, @Language("kotlin") before: String, @Language("kotlin") after: String) {
        val configuration = Configuration.getProjectInstance(project).advancedConfiguration
        val allowed = configuration.isSourceModificationAllowed

        configuration.isSourceModificationAllowed = true
        try {
            myFixture.configureByText("${getTestName(true)}.kt", before.trimIndent())
            InjectLanguageAction.invokeImpl(project, myFixture.editor, myFixture.file, injectable)
            myFixture.checkResult(after.trimIndent())
        }
        finally {
            configuration.isSourceModificationAllowed = allowed
        }
    }

    fun range(start: Int, end: Int) = TextRange.create(start, end)
}