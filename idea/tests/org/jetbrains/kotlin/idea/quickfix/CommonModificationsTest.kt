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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.JvmCommonIntentionActionsFactory
import com.intellij.lang.Language
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiModifier
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.convert
import org.junit.Assert


class CommonModificationsTest : LightPlatformCodeInsightFixtureTestCase() {

    fun testMakeNotFinal() {
        myFixture.configureByText("foo.kt", """
        class Foo {
            fun bar<caret>(){}
        }
        """)

        myFixture.launchAction(codeModifications.createChangeModifierAction(uastElementAtCaret(myFixture), PsiModifier.FINAL, false )!!)
        myFixture.checkResult("""
        class Foo {
            open fun bar(){}
        }
        """)
    }

    fun testMakePrivate() {
        myFixture.configureByText("foo.kt", """
        class Foo<caret> {
            fun bar(){}
        }
        """)

        myFixture.launchAction(codeModifications.createChangeModifierAction(uastElementAtCaret(myFixture), PsiModifier.PRIVATE, true )!!)
        myFixture.checkResult("""
        private class Foo {
            fun bar(){}
        }
        """)
    }

    fun testMakeNotPrivate() {
        myFixture.configureByText("foo.kt", """
        private class Foo<caret> {
            fun bar(){}
        }
        """.trim())

        myFixture.launchAction(codeModifications.createChangeModifierAction(uastElementAtCaret(myFixture), PsiModifier.PRIVATE, false )!!)
        myFixture.checkResult("""
        class Foo {
            fun bar(){}
        }
        """.trim(), true)
    }

    fun testDontMakeFunInObjectsOpen() {
        myFixture.configureByText("foo.kt", """
        object Foo {
            fun bar<caret>(){}
        }
        """.trim())
        Assert.assertNull(codeModifications.createChangeModifierAction(uastElementAtCaret(myFixture), PsiModifier.FINAL, false))
    }

    private fun uastElementAtCaret(myFixture: CodeInsightTestFixture): UDeclaration {
        val elementAtCaret = myFixture.elementAtCaret
        val uastContext = ServiceManager.getService(elementAtCaret.project, UastContext::class.java) ?: error("UastContext not found")
        val uastLanguagePlugin = uastContext.findPlugin(elementAtCaret) ?: error("Language plugin was not found for $this (${this.javaClass.name})")
        return uastLanguagePlugin.convert<UDeclaration>(elementAtCaret, null)
    }

    private val codeModifications: JvmCommonIntentionActionsFactory
        get() = JvmCommonIntentionActionsFactory.forLanguage(Language.findLanguageByID("kotlin")!!)

}

