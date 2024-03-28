/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerParamTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    private fun composerParam(
        @Language("kotlin")
        source: String,
        validator: (element: IrElement) -> Unit = { },
        dumpTree: Boolean = false
    ) = verifyGoldenComposeIrTransform(
        """
            @file:OptIn(
              InternalComposeApi::class,
            )
            package test

            import androidx.compose.runtime.InternalComposeApi
            import androidx.compose.runtime.ComposeCompilerApi
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            $source
        """.trimIndent(),
        """
            package test
            fun used(x: Any?) {}
        """,
        validator,
        dumpTree
    )

    @Test
    fun testCallingProperties(): Unit = composerParam(
        """
            val bar: Int @Composable get() { return 123 }

            @NonRestartableComposable @Composable fun Example() {
                bar
            }
        """
    )

    @Test
    fun testAbstractComposable(): Unit = composerParam(
        """
            abstract class BaseFoo {
                @NonRestartableComposable
                @Composable
                abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @NonRestartableComposable
                @Composable
                override fun bar() {}
            }
        """
    )

    @Test
    fun testLocalClassAndObjectLiterals(): Unit = composerParam(
        """
            @NonRestartableComposable
            @Composable
            fun Wat() {}

            @NonRestartableComposable
            @Composable
            fun Foo(x: Int) {
                Wat()
                @NonRestartableComposable
                @Composable fun goo() { Wat() }
                class Bar {
                    @NonRestartableComposable
                    @Composable fun baz() { Wat() }
                }
                goo()
                Bar().baz()
            }
        """
    )

    @Test
    fun testVarargWithNoArgs(): Unit = composerParam(
        """
            @Composable
            fun VarArgsFirst(vararg foo: Any?) {
                println(foo)
            }

            @Composable
            fun VarArgsCaller() {
                VarArgsFirst()
            }
            """
    )

    // Regression test for b/286132194
    @Test
    fun testStableVarargParams(): Unit = composerParam(
        """
            @Composable
            fun B(vararg values: Int) {
                print(values)
            }

            @NonRestartableComposable
            @Composable
            fun Test() {
                B(0, 1, 2, 3)
            }
        """
    )

    @Test
    fun testNonComposableCode(): Unit = composerParam(
        """
            fun A() {}
            val b: Int get() = 123
            fun C(x: Int) {
                var x = 0
                x++

                class D {
                    fun E() { A() }
                    val F: Int get() = 123
                }
                val g = object { fun H() {} }
            }
            fun I(block: () -> Unit) { block() }
            fun J() {
                I {
                    I {
                        A()
                    }
                }
            }
        """
    )

    @Test
    fun testCircularCall(): Unit = composerParam(
        """
            @NonRestartableComposable
            @Composable fun Example() {
                Example()
            }
        """
    )

    @Test
    fun testInlineCall(): Unit = composerParam(
        """
            @Composable inline fun Example(content: @Composable () -> Unit) {
                content()
            }

            @NonRestartableComposable
            @Composable fun Test() {
                Example {}
            }
        """
    )

    @Test
    fun testDexNaming(): Unit = composerParam(
        """
            val myProperty: () -> Unit @Composable get() {
                return {  }
            }
        """
    )

    @Test
    fun testInnerClass(): Unit = composerParam(
        """
            interface A {
                fun b() {}
            }
            class C {
                val foo = 1
                inner class D : A {
                    override fun b() {
                        print(foo)
                    }
                }
            }
        """
    )

    @Test
    fun testKeyCall() {
        composerParam(
            """
                import androidx.compose.runtime.key

                @Composable
                fun Wrapper(block: @Composable () -> Unit) {
                    block()
                }

                @Composable
                fun Leaf(text: String) {
                    used(text)
                }

                @Composable
                fun Test(value: Int) {
                    key(value) {
                        Wrapper {
                            Leaf("Value ${'$'}value")
                        }
                    }
                }
            """,
            validator = { element ->
                // Validate that no composers are captured by nested lambdas
                var currentComposer: IrValueParameter? = null
                element.accept(
                    object : IrElementVisitorVoid {
                        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                            val composer = declaration.valueParameters.firstOrNull {
                                it.name == KtxNameConventions.COMPOSER_PARAMETER
                            }
                            val oldComposer = currentComposer
                            if (composer != null) currentComposer = composer
                            super.visitSimpleFunction(declaration)
                            currentComposer = oldComposer
                        }

                        override fun visitElement(element: IrElement) {
                            element.acceptChildren(this, null)
                        }

                        override fun visitGetValue(expression: IrGetValue) {
                            super.visitGetValue(expression)
                            val value = expression.symbol.owner
                            if (
                                value is IrValueParameter && value.name ==
                                KtxNameConventions.COMPOSER_PARAMETER
                            ) {
                                assertEquals(
                                    "Composer unexpectedly captured",
                                    currentComposer,
                                    value
                                )
                            }
                        }
                    },
                    null
                )
            }
        )
    }

    @Test
    fun testComposableNestedCall() {
        composerParam(
            """
                @Composable
                fun composeVector(
                    composable: @Composable () -> Unit
                ) {
                    emit {
                        emit {
                            composable()
                        }
                    }
                }
                @Composable
                inline fun emit(composable: @Composable () -> Unit) {
                    composable()
                }
            """
        )
    }

    @Test
    fun testDelegateCall() {
        composerParam(
            """
                import kotlin.reflect.KProperty

                class Foo
                @Composable
                operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>): Foo = this

                class FooDelegate {
                    @Composable
                    operator fun getValue(thisObj: Any?, property: KProperty<*>): FooDelegate = this
                }

                class Bar {
                    @get:Composable
                    val foo by Foo()
                }

                @Composable
                fun test() {
                    val foo by Foo()
                    val fooDelegate by FooDelegate()
                    val bar = Bar()
                    println(foo)
                    println(fooDelegate)
                    println(bar.foo)
                }
            """,
        )
    }

    @Test
    fun testUnstableDelegateCall() = composerParam(
        """
                import kotlin.reflect.KProperty

                class Foo {
                    var unstableField: Int = 0
                }

                @Composable
                inline operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>): Foo = this

                @Composable
                fun test() {
                    val foo by Foo()
                    println(foo)
                }
            """
    )

    @Test
    fun testStableDelegateCall() = composerParam(
        """
            import kotlin.reflect.KProperty

            class Foo

            @Composable
            inline operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>): Foo = this

            @Composable
            fun test(foo: Foo) {
                val delegated by foo
                used(delegated)
            }
        """
    )

    @Test
    fun validateNoComposableFunctionSymbolCalls() = composerParam(
        source = """
            fun abc0(l: @Composable () -> Unit) {
                val hc = l.hashCode()
            }
            fun abc1(l: @Composable (String) -> Unit) {
                val hc = l.hashCode()
            }
            fun abc2(l: @Composable (String, Int) -> Unit) {
                val hc = l.hashCode()
            }
            fun abc3(
                l: @Composable (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) -> Any
            ) {
                val hc = l.hashCode()
            }
        """.trimIndent(),
        validator = {
            val expectedArity = listOf(2, 3, 4, 15)
            var i = 0 // to iterate over `hashCode` calls
            it.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    if (expression.symbol.owner.name.asString() == "hashCode") {
                        assertEquals(
                            "kotlin.Function${expectedArity[i]}.hashCode",
                            expression.symbol.owner.fqNameForIrSerialization.asString())
                        i++
                    }
                }
            })
        }
    )

    @Test
    fun validateNoComposableFunctionReferencesInOverriddenSymbols() =
        verifyGoldenCrossModuleComposeIrTransform(
            dependencySource = """
            package dependency

            import androidx.compose.runtime.Composable

            interface Content {
                fun setContent(c: @Composable () -> Unit)
            }
        """.trimIndent(),
            source = """
            package test

            import androidx.compose.runtime.Composable
            import dependency.Content

            class ContentImpl : Content {
                override fun setContent(c: @Composable () -> Unit) {}
            }
        """.trimIndent(),
            validator = {
                it.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    private val targetFqName = "test.ContentImpl.setContent"

                    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                        if (declaration.fqNameForIrSerialization.asString() == targetFqName) {
                            assertEquals(1, declaration.overriddenSymbols.size)
                            val firstParameterOfOverridden =
                                declaration.overriddenSymbols.first().owner.valueParameters.first()
                                    .takeIf { it.name.asString() == "c" }!!
                            assertEquals(
                                "kotlin.Function2",
                                firstParameterOfOverridden.type.classFqName?.asString()
                            )
                        }
                    }
                })
            }
        )

    @Test
    fun validateNoComposableFunctionReferencesInCalleeOverriddenSymbols() =
        verifyGoldenCrossModuleComposeIrTransform(
            dependencySource = """
            package dependency

            import androidx.compose.runtime.Composable

            interface Content {
                fun setContent(c: @Composable () -> Unit = {})
            }
            class ContentImpl : Content {
                override fun setContent(c: @Composable () -> Unit) {}
            }
        """.trimIndent(),
            source = """
            package test

            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import dependency.ContentImpl

            @Composable
            @NonRestartableComposable
            fun Foo() {
                ContentImpl().setContent()
            }
        """.trimIndent(),
            validator = {
                it.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    private val targetFqName = "dependency.ContentImpl.setContent"

                    override fun visitCall(expression: IrCall) {
                        val callee = expression.symbol.owner
                        if (callee.fqNameForIrSerialization.asString() == targetFqName) {
                            val firstParameterOfOverridden =
                                callee.overriddenSymbols.first().owner.valueParameters.first()
                                    .takeIf { it.name.asString() == "c" }!!
                            assertEquals(
                                "kotlin.Function2",
                                firstParameterOfOverridden.type.classFqName?.asString()
                            )
                        }
                        super.visitCall(expression)
                    }
                })
            }
        )

    @Test
    fun composableCallInAnonymousObjectInitializer() =
        verifyGoldenComposeIrTransform(
            extra = """
                import androidx.compose.runtime.*

                @Composable fun Foo(): State<Int> = TODO()
            """,
            source = """
                import androidx.compose.runtime.*

                @Composable fun Test(inputs: List<Int>) {
                    val objs = inputs.map {
                        object {
                            init {
                                Foo()
                            }

                            val state = Foo()
                            val value by Foo()
                        }
                    }
                    objs.forEach {
                        println(it.state)
                        println(it.value)
                    }
                }
            """
        )

    @Test
    fun composableLocalFunctionInsideLocalClass() =
        verifyGoldenComposeIrTransform(
            extra = """
                import androidx.compose.runtime.*

                abstract class C {
                    @Composable
                    abstract fun Render()
                }

                @Composable fun Button(onClick: () -> Unit, content: @Composable () -> Unit) {}
            """,
            source = """
                import androidx.compose.runtime.*

                fun test() {
                    object: C() {
                        @Composable
                        override fun Render() {
                            @Composable
                            fun B() {
                                Button({}) {}
                            }

                            B()
                        }
                    }
                }
            """
        )

    @Test
    fun composeValueClassDefaultParameter() =
        verifyGoldenComposeIrTransform(
            extra = """
                @JvmInline
                value class Data(val string: String)
                @JvmInline
                value class IntData(val value: Int)
            """,
            source = """
                import androidx.compose.runtime.*

                @Composable fun Example(data: Data = Data(""), intData: IntData = IntData(0)) {}
            """
        )
}
