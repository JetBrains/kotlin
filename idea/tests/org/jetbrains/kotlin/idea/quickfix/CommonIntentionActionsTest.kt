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
import com.intellij.lang.jvm.JvmClass
import com.intellij.lang.jvm.JvmElement
import com.intellij.lang.jvm.JvmModifier
import com.intellij.lang.jvm.actions.*
import com.intellij.lang.jvm.types.JvmSubstitutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair.pair
import com.intellij.psi.PsiJvmSubstitutor
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.uast.toUElement
import org.junit.Assert

class CommonIntentionActionsTest : LightPlatformCodeInsightFixtureTestCase() {
    private class SimpleMethodRequest(
        project: Project,
        private val methodName: String,
        private val modifiers: Collection<JvmModifier> = emptyList(),
        private val returnType: ExpectedTypes = emptyList(),
        private val annotations: Collection<AnnotationRequest> = emptyList(),
        parameters: List<ExpectedParameter> = emptyList(),
        private val targetSubstitutor: JvmSubstitutor = PsiJvmSubstitutor(project, PsiSubstitutor.EMPTY)
    ) : CreateMethodRequest {
        private val expectedParameters = parameters

        override fun getTargetSubstitutor(): JvmSubstitutor = targetSubstitutor

        override fun getModifiers() = modifiers

        override fun getMethodName() = methodName

        override fun getAnnotations() = annotations

        override fun getExpectedParameters(): List<ExpectedParameter> = expectedParameters

        override fun getReturnType() = returnType

        override fun isValid(): Boolean = true

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

    fun testMakePrivatePublic() {
        myFixture.configureByText(
            "foo.kt", """class Foo {
                        |    private fun <caret>bar(){}
                        |}""".trim().trimMargin()
        )

        myFixture.launchAction(
            createModifierActions(
                myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PUBLIC, true)
            ).findWithText("Remove 'private' modifier")
        )
        myFixture.checkResult(
            """class Foo {
              |    fun <caret>bar(){}
              |}""".trim().trimMargin(), true
        )
    }

    fun testMakeProtectedPublic() {
        myFixture.configureByText(
            "foo.kt", """open class Foo {
                        |    protected fun <caret>bar(){}
                        |}""".trim().trimMargin()
        )

        myFixture.launchAction(
            createModifierActions(
                myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PUBLIC, true)
            ).findWithText("Remove 'protected' modifier")
        )
        myFixture.checkResult(
            """open class Foo {
              |    fun <caret>bar(){}
              |}""".trim().trimMargin(), true
        )
    }

    fun testMakeInternalPublic() {
        myFixture.configureByText(
            "foo.kt", """class Foo {
                        |    internal fun <caret>bar(){}
                        |}""".trim().trimMargin()
        )

        myFixture.launchAction(
            createModifierActions(
                myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PUBLIC, true)
            ).findWithText("Remove 'internal' modifier")
        )
        myFixture.checkResult(
            """class Foo {
              |    fun <caret>bar(){}
              |}""".trim().trimMargin(), true
        )
    }

    fun testAddAnnotation() {
        myFixture.configureByText(
            "foo.kt", """class Foo {
                        |   fun <caret>bar(){}
                        |}""".trim().trimMargin()
        )

        myFixture.launchAction(
            createAddAnnotationActions(
                myFixture.findElementByText("bar", KtModifierListOwner::class.java).toLightElements().single() as PsiMethod,
                annotationRequest("kotlin.jvm.JvmName", stringAttribute("name", "foo"))
            ).single()
        )
        myFixture.checkResult(
            """class Foo {
              |   @JvmName(name = "foo")
              |   fun <caret>bar(){}
              |}""".trim().trimMargin(), true
        )
    }

    fun testAddJavaAnnotationValue() {

        myFixture.addFileToProject(
            "pkg/myannotation/JavaAnnotation.java", """
            package pkg.myannotation

            public @interface JavaAnnotation {
                String value();
                int param() default 0;
            }
        """.trimIndent()
        )

        myFixture.configureByText(
            "foo.kt", """class Foo {
                        |   fun bar(){}
                        |   fun baz(){}
                        |}""".trim().trimMargin()
        )

        myFixture.launchAction(
            createAddAnnotationActions(
                myFixture.findElementByText("bar", KtModifierListOwner::class.java).toLightElements().single() as PsiMethod,
                annotationRequest("pkg.myannotation.JavaAnnotation", stringAttribute("value", "foo"), intAttribute("param", 2))
            ).single()
        )
        myFixture.launchAction(
            createAddAnnotationActions(
                myFixture.findElementByText("baz", KtModifierListOwner::class.java).toLightElements().single() as PsiMethod,
                annotationRequest("pkg.myannotation.JavaAnnotation", intAttribute("param", 2), stringAttribute("value", "foo"))
            ).single()
        )
        myFixture.checkResult(
            """import pkg.myannotation.JavaAnnotation
                |
                |class Foo {
                |   @JavaAnnotation("foo", param = 2)
                |   fun bar(){}
                |   @JavaAnnotation(param = 2, value = "foo")
                |   fun baz(){}
                |}""".trim().trimMargin(), true
        )
    }

    fun testDontMakePublicPublic() {
        myFixture.configureByText(
            "foo.kt", """class Foo {
                        |    fun <caret>bar(){}
                        |}""".trim().trimMargin()
        )

        assertEmpty(createModifierActions(myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.PUBLIC, true)))
    }

    fun testDontMakeFunInObjectsOpen() {
        myFixture.configureByText("foo.kt", """
        object Foo {
            fun bar<caret>(){}
        }
        """.trim())
        assertEmpty(createModifierActions(myFixture.atCaret(), MemberRequest.Modifier(JvmModifier.FINAL, false)))
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
                    myFixture.atCaret(), constructorRequest(project, listOf(pair("param0", PsiType.INT as PsiType)))
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
                    constructorRequest(project, listOf(pair("param0", PsiType.INT as PsiType)))
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
                    constructorRequest(project, listOf(pair("param0", PsiType.INT as PsiType)))
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
                    constructorRequest(project, emptyList())
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
            createMethodActions(
                myFixture.atCaret(),
                SimpleMethodRequest(
                    project,
                    methodName = "setBaz",
                    modifiers = listOf(JvmModifier.PUBLIC),
                    returnType = expectedTypes(),
                    parameters = expectedParams(PsiType.getTypeByName("java.lang.String", project, project.allScope()))
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
            createMethodActions(
                myFixture.atCaret(),
                SimpleMethodRequest(
                    project,
                    methodName = "setBaz",
                    modifiers = listOf(JvmModifier.PUBLIC),
                    returnType = expectedTypes(),
                    parameters = expectedParams(PsiType.getTypeByName("java.lang.String", project, project.allScope()))
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

    fun testAddStringVarField() {
        myFixture.configureByText(
            "foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin()
        )
        myFixture.launchAction(
            createFieldActions(
                myFixture.atCaret(),
                FieldRequest(project, emptyList(), "java.util.Date", "baz")
            ).findWithText("Add 'var' property 'baz' to 'Foo'")
        )
        myFixture.checkResult(
            """
        |import java.util.Date
        |
        |class Foo {
        |    @JvmField
        |    var baz: Date = TODO("initialize me")
        |
        |    fun bar() {}
        |}
        """.trim().trimMargin(), true
        )
    }

    fun testAddLateInitStringVarField() {
        myFixture.configureByText(
            "foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin()
        )

        myFixture.launchAction(
            createFieldActions(
                myFixture.atCaret(),
                FieldRequest(project, listOf(JvmModifier.PRIVATE), "java.lang.String", "baz")
            ).findWithText("Add 'lateinit var' property 'baz' to 'Foo'")
        )
        myFixture.checkResult(
            """
        |class Foo {
        |    private lateinit var baz: String
        |
        |    fun bar() {}
        |}
        """.trim().trimMargin(), true
        )
    }


    private fun createFieldActions(atCaret: JvmClass, fieldRequest: CreateFieldRequest): List<IntentionAction> =
        com.intellij.lang.jvm.actions.EP_NAME.extensions.flatMap { it.createAddFieldActions(atCaret, fieldRequest) }

    fun testAddStringValProperty() {
        myFixture.configureByText("foo.kt", """
        |class Foo<caret> {
        |    fun bar() {}
        |}
        """.trim().trimMargin())

        myFixture.launchAction(
            createMethodActions(
                myFixture.atCaret(),
                SimpleMethodRequest(
                    project,
                    methodName = "getBaz",
                    modifiers = listOf(JvmModifier.PUBLIC),
                    returnType = expectedTypes(PsiType.getTypeByName("java.lang.String", project, project.allScope())),
                    parameters = expectedParams()
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

    private fun expectedTypes(vararg psiTypes: PsiType) = psiTypes.map { expectedType(it) }

    private fun expectedParams(vararg psyTypes: PsiType) =
        psyTypes.mapIndexed { index, psiType -> expectedParameter(expectedTypes(psiType), "param$index") }

    private inline fun <reified T : JvmElement> CodeInsightTestFixture.atCaret() = elementAtCaret.toUElement() as T

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun List<IntentionAction>.findWithText(text: String): IntentionAction =
            this.firstOrNull { it.text == text } ?:
            Assert.fail("intention with text '$text' was not found, only ${this.joinToString { "\"${it.text}\"" }} available") as Nothing

    class FieldRequest(
        private val project: Project,
        val modifiers: List<JvmModifier>,
        val type: String,
        val name: String
    ) : CreateFieldRequest {
        override fun getTargetSubstitutor(): JvmSubstitutor = PsiJvmSubstitutor(project, PsiSubstitutor.EMPTY)

        override fun getModifiers(): Collection<JvmModifier> = modifiers

        override fun isConstant(): Boolean = false

        override fun getFieldType(): List<ExpectedType> =
            com.intellij.lang.jvm.actions.expectedTypes(PsiType.getTypeByName(type, project, project.allScope()))

        override fun getFieldName(): String = name

        override fun isValid(): Boolean = true
    }


}


