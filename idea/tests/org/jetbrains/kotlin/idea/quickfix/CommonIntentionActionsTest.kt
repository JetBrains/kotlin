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
import com.intellij.lang.jvm.JvmElement
import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.actions.*
import com.intellij.lang.jvm.types.JvmSubstitutor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJvmSubstitutor
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UastContext
import org.jetbrains.uast.toUElement
import org.junit.Assert

class CommonIntentionActionsTest : LightPlatformCodeInsightFixtureTestCase() {
    private class SimpleMethodRequest(
            project: Project,
            override val methodName: String,
            override val modifiers: Collection<JvmModifier> = emptyList(),
            override val returnType: ExpectedTypes = emptyList(),
            override val annotations: Collection<AnnotationRequest> = emptyList(),
            override val parameters: List<ExpectedParameter> = emptyList(),
            override val targetSubstitutor: JvmSubstitutor = PsiJvmSubstitutor(project, PsiSubstitutor.EMPTY)
    ) : CreateMethodRequest {
        override val isValid: Boolean = true
    }

    private class NameInfo(vararg names: String) : SuggestedNameInfo(names)

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    fun testMakeNotFinal() {
        myFixture.configureByText("foo.kt", """
        class Foo {
            fun bar<caret>(){}
        }
        """)

        myFixture.launchAction(
                createModifierActions(
                        myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.FINAL, false)
                ).findWithText("Make 'bar' open")
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
                createModifierActions(
                        myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PRIVATE, true)
                ).findWithText("Make 'Foo' private")
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
                createModifierActions(
                        myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PRIVATE, false)
                ).findWithText("Remove 'private' modifier")
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
        Assert.assertTrue(createModifierActions(myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.FINAL, false)).isEmpty())
    }

    fun testAddVoidVoidMethod() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
                createMethodActions(
                        myFixture.atCaret(),
                        methodRequest(project, "baz", JvmModifier.PRIVATE, PsiType.VOID)
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
                createMethodActions(
                        myFixture.atCaret(),
                        SimpleMethodRequest(project,
                                            methodName = "baz",
                                            modifiers = listOf(JvmModifier.PUBLIC),
                                            returnType = expectedTypes(PsiType.INT),
                                            parameters = expectedParams(PsiType.INT))
                ).findWithText("Add method 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    fun bar() {}
        |    fun baz(param0: Int): Int {
        |        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
                createConstructorActions(
                        myFixture.atCaret(),
                        MemberRequest.Constructor(parameters = makeParams(PsiType.INT))
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
                createConstructorActions(
                        myFixture.atCaret(),
                        MemberRequest.Constructor(parameters = makeParams(PsiType.INT))
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
                createConstructorActions(
                        myFixture.atCaret(),
                        MemberRequest.Constructor(parameters = makeParams(PsiType.INT))
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
                createConstructorActions(
                        myFixture.atCaret(),
                        MemberRequest.Constructor()
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
                createPropertyActions(
                        myFixture.atCaret(),
                        MemberRequest.Property(
                                propertyName = "baz",
                                visibilityModifier = JvmModifier.PUBLIC,
                                propertyType = PsiType.getTypeByName("java.lang.String", project, project.allScope()),
                                getterRequired = true,
                                setterRequired = true
                        )
                ).findWithText("Add 'var' property 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    var baz: String = TODO("initialize me")
        |
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

        myFixture.launchAction(
                createPropertyActions(
                        myFixture.atCaret(),
                        MemberRequest.Property(
                                propertyName = "baz",
                                visibilityModifier = JvmModifier.PUBLIC,
                                propertyType = PsiType.getTypeByName("java.lang.String", project, project.allScope()),
                                getterRequired = true,
                                setterRequired = true
                        )
                ).findWithText("Add 'lateinit var' property 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    lateinit var baz: String
        |
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
                createPropertyActions(
                        myFixture.atCaret(),
                        MemberRequest.Property(
                                propertyName = "baz",
                                visibilityModifier = JvmModifier.PUBLIC,
                                propertyType = PsiType.getTypeByName("java.lang.String", project, project.allScope()),
                                getterRequired = true,
                                setterRequired = false
                        )
                ).findWithText("Add 'val' property 'baz' to 'Foo'")
        )
        myFixture.checkResult("""
        |class Foo {
        |    val baz: String = TODO("initialize me")
        |
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

    private fun expectedTypes(vararg psiTypes: PsiType) = psiTypes.map { expectedType(it) }

    private fun expectedParams(vararg psyTypes: PsiType) =
            psyTypes.mapIndexed { index, psiType -> NameInfo("param$index") to expectedTypes(psiType) }

    private inline fun <reified T : JvmElement> CodeInsightTestFixture.atCaret() = elementAtCaret.toUElement() as T

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun List<IntentionAction>.findWithText(text: String): IntentionAction =
            this.firstOrNull { it.text == text } ?:
            Assert.fail("intention with text '$text' was not found, only ${this.joinToString { "\"${it.text}\"" }} available") as Nothing
}

