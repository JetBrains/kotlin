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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.JvmCommonIntentionActionsFactory
import com.intellij.codeInsight.intention.MethodInsertionInfo
import com.intellij.lang.Language
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.uast.*
import org.junit.Assert

class CommonIntentionActionsTest : LightPlatformCodeInsightFixtureTestCase() {
    fun testMakeNotFinal() {
        myFixture.configureByText("foo.kt", """
        class Foo {
            fun bar<caret>(){}
        }
        """)

        myFixture.launchAction(
                codeModifications
                        .createChangeModifierAction(atCaret(myFixture), PsiModifier.FINAL, false)!!
                        .ensureHasText("Make 'bar' open")
        )
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

        myFixture.launchAction(
                codeModifications
                        .createChangeModifierAction(atCaret(myFixture), PsiModifier.PRIVATE, true)!!
                        .ensureHasText("Make 'Foo' private")
        )
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

        myFixture.launchAction(
                codeModifications
                        .createChangeModifierAction(atCaret(myFixture), PsiModifier.PRIVATE, false)!!
                        .ensureHasText("Remove 'private' modifier")
        )
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
        Assert.assertNull(codeModifications.createChangeModifierAction(atCaret(myFixture), PsiModifier.FINAL, false))
    }

    fun testAddVoidVoidMethod() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.simpleMethodInfo(atCaret(myFixture), "baz", PsiModifier.PRIVATE, PsiType.VOID, emptyList())
                ).findWithText("Add method 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    fun bar() {}
        |    private fun baz() {
        |
        |    }
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddIntIntMethod() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.simpleMethodInfo(atCaret(myFixture), "baz", PsiModifier.PUBLIC, PsiType.INT, makeParams(PsiType.INT))
                ).findWithText("Add method 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    fun bar() {}
        |    fun baz(param0: Int): Int {
        |
        |    }
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddIntPrimaryConstructor() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.constructorInfo(atCaret(myFixture), makeParams(PsiType.INT))
                ).findWithText("Add primary constructor to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo(param0: Int) {
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddIntSecondaryConstructor() {
        myFixture.configureByText("foo.kt", """
        |class <caret>Foo() {
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.constructorInfo(atCaret(myFixture), makeParams(PsiType.INT))
                ).findWithText("Add secondary constructor to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo() {
        |    constructor(param0: Int) {
        |
        |    }
        |}
        """.trim().trimMargin(), true)
    }

    fun testChangePrimaryConstructorInt() {
        myFixture.configureByText("foo.kt", """
        |class <caret>Foo() {
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.constructorInfo(atCaret(myFixture), makeParams(PsiType.INT))
                ).findWithText("Add 'int' as 1st parameter to method 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo(param0: Int) {
        |}
        """.trim().trimMargin(), true)
    }

    fun testRemoveConstructorParameters() {
        myFixture.configureByText("foo.kt", """
        |class <caret>Foo(i: Int) {
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddCallableMemberActions(
                        MethodInsertionInfo.constructorInfo(atCaret(myFixture), makeParams())
                ).findWithText("Remove 1st parameter from method 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo() {
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddStringVarProperty() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddBeanPropertyActions(
                        atCaret(myFixture),
                        "baz",
                        PsiModifier.PUBLIC,
                        PsiType.getTypeByName("java.lang.String", project, GlobalSearchScope.allScope(project)),
                        true,
                        true
                ).findWithText("Add 'var' property 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    var baz: String = TODO("initialize me")
        |    fun bar() {}
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddLateInitStringVarProperty() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(codeModifications.createAddBeanPropertyActions(
                atCaret(myFixture), "baz", PsiModifier.PUBLIC, PsiType.getTypeByName("java.lang.String", project, GlobalSearchScope.allScope(project)), true, true)
                                       .findWithText("Add 'lateinit var' property 'baz' to 'Foo'"))
        myFixture.checkResult("""
        |class Foo {
        |    lateinit var baz: String
        |    fun bar() {}
        |}
        """.trim().trimMargin(), true)
    }

    fun testAddStringValProperty() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                codeModifications.createAddBeanPropertyActions(
                        atCaret(myFixture),
                        "baz",
                        PsiModifier.PUBLIC,
                        PsiType.getTypeByName("java.lang.String", project, GlobalSearchScope.allScope(project)),
                        false,
                        true
                ).findWithText("Add 'val' property 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    val baz: String = TODO("initialize me")
        |    fun bar() {}
        |}
        """.trim().trimMargin(), true)
    }

    private fun makeParams(vararg psyTypes: PsiType): List<UParameter> {
        val uastContext = UastContext(myFixture.project)
        val factory = JavaPsiFacade.getElementFactory(myFixture.project)
        val parameters = psyTypes.mapIndexed { index, psiType -> factory.createParameter("param$index", psiType) }
        return parameters.map { uastContext.convertElement(it, null, UParameter::class.java) as UParameter }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : UElement> atCaret(myFixture: CodeInsightTestFixture): T {
        return myFixture.elementAtCaret.toUElement() as T
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun List<IntentionAction>.findWithText(text: String): IntentionAction =
            this.firstOrNull { it.text == text } ?:
            Assert.fail("intention with text '$text' was not found, only ${this.joinToString { "\"${it.text}\"" }} available") as Nothing

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun IntentionAction.ensureHasText(text: String): IntentionAction =
            if (this.text == text) this else Assert.fail("intention with text '$text' was not found, only \"${this.text}\" available") as Nothing

    private val codeModifications: JvmCommonIntentionActionsFactory
        get() = JvmCommonIntentionActionsFactory.forLanguage(Language.findLanguageByID("kotlin")!!)!!

}

