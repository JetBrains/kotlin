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

package androidx.compose.compiler.plugins.kotlin

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.junit.Test

class ComposerParamTransformTests : ComposeIrTransformTest() {
    private fun composerParam(
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
        validator: (element: IrElement) -> Unit = { },
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
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
        expectedTransformed,
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
        """,
        """
            val bar: Int
              @Composable @JvmName(name = "getBar")
              get() {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C:Test.kt#2487m")
                val tmp0 = 123
                %composer.endReplaceableGroup()
                return tmp0
              }
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<bar>:Test.kt#2487m")
              bar
              %composer.endReplaceableGroup()
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
        """,
        """
            @StabilityInferred(parameters = 0)
            abstract class BaseFoo {
              @NonRestartableComposable
              @Composable
              abstract fun bar(%composer: Composer?, %changed: Int)
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class FooImpl : BaseFoo {
              @NonRestartableComposable
              @Composable
              override fun bar(%composer: Composer?, %changed: Int) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(bar):Test.kt#2487m")
                %composer.endReplaceableGroup()
              }
              static val %stable: Int = 0
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
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Wat(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Wat):Test.kt#2487m")
              %composer.endReplaceableGroup()
            }
            @NonRestartableComposable
            @Composable
            fun Foo(x: Int, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Foo)<Wat()>,<goo()>,<baz()>:Test.kt#2487m")
              Wat(%composer, 0)
              @NonRestartableComposable
              @Composable
              fun goo(%composer: Composer?, %changed: Int) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(goo)<Wat()>:Test.kt#2487m")
                Wat(%composer, 0)
                %composer.endReplaceableGroup()
              }
              class Bar {
                @NonRestartableComposable
                @Composable
                fun baz(%composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(baz)<Wat()>:Test.kt#2487m")
                  Wat(%composer, 0)
                  %composer.endReplaceableGroup()
                }
              }
              goo(%composer, 0)
              Bar().baz(%composer, 0)
              %composer.endReplaceableGroup()
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
        """,
        """
            @Composable
            fun VarArgsFirst(foo: Array<out Any?>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(VarArgsFirst):Test.kt#2487m")
              println(foo)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                VarArgsFirst(*foo, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun VarArgsCaller(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(VarArgsCaller)<VarArg...>:Test.kt#2487m")
              if (%changed !== 0 || !%composer.skipping) {
                VarArgsFirst(
                  %composer = %composer,
                  %changed = 8
                )
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                VarArgsCaller(%composer, %changed or 0b0001)
              }
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
        """,
        """
            fun A() { }
            val b: Int
              get() {
                return 123
              }
            fun C(x: Int) {
              var x = 0
              x++
              class D {
                fun E() {
                  A()
                }
                val F: Int
                  get() {
                    return 123
                  }
              }
              val g = object {
                fun H() { }
              }
            }
            fun I(block: Function0<Unit>) {
              block()
            }
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
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Example(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<Exampl...>:Test.kt#2487m")
              Example(%composer, 0)
              %composer.endReplaceableGroup()
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
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Example(content: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Example)<conten...>:Test.kt#2487m")
              content(%composer, 0b1110 and %changed)
              %composer.endReplaceableGroup()
            }
            @NonRestartableComposable
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test)<Exampl...>:Test.kt#2487m")
              Example({ %composer: Composer?, %changed: Int ->
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C:Test.kt#2487m")
                if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                  Unit
                } else {
                  %composer.skipToGroupEnd()
                }
                %composer.endReplaceableGroup()
              }, %composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testDexNaming(): Unit = composerParam(
        """
            val myProperty: () -> Unit @Composable get() {
                return {  }
            }
        """,
        """
            val myProperty: Function0<Unit>
              @Composable @JvmName(name = "getMyProperty")
              get() {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C:Test.kt#2487m")
                val tmp0 = {
                }
                %composer.endReplaceableGroup()
                return tmp0
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
        """,
        """
            interface A {
              open fun b() { }
            }
            @StabilityInferred(parameters = 0)
            class C {
              val foo: Int = 1
              inner class D : A {
                override fun b() {
                  print(foo)
                }
              }
              static val %stable: Int = 0
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
            """
                @Composable
                @ComposableInferredTarget(scheme = "[0[0]]")
                fun Wrapper(block: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Wrapper)<block(...>:Test.kt#2487m")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(block)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                    block(%composer, 0b1110 and %dirty)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Wrapper(block, %composer, %changed or 0b0001)
                  }
                }
                @Composable
                fun Leaf(text: String, %composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Leaf):Test.kt#2487m")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(text)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                    used(text)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Leaf(text, %composer, %changed or 0b0001)
                  }
                }
                @Composable
                fun Test(value: Int, %composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(Test):Test.kt#2487m")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                    %composer.startMovableGroup(<>, value)
                    sourceInformation(%composer, "<Wrappe...>")
                    Wrapper(composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                      sourceInformation(%composer, "C<Leaf("...>:Test.kt#2487m")
                      if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                        Leaf("Value %value", %composer, 0)
                      } else {
                        %composer.skipToGroupEnd()
                      }
                    }, %composer, 0b0110)
                    %composer.endMovableGroup()
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Test(value, %composer, %changed or 0b0001)
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
            """,
            """
                @Composable
                @ComposableInferredTarget(scheme = "[0[0]]")
                fun composeVector(composable: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  %composer = %composer.startRestartGroup(<>)
                  sourceInformation(%composer, "C(composeVector)<emit>:Test.kt#2487m")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(composable)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                    emit({ %composer: Composer?, %changed: Int ->
                      %composer.startReplaceableGroup(<>)
                      sourceInformation(%composer, "C<emit>:Test.kt#2487m")
                      if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                        emit({ %composer: Composer?, %changed: Int ->
                          %composer.startReplaceableGroup(<>)
                          sourceInformation(%composer, "C<compos...>:Test.kt#2487m")
                          if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                            composable(%composer, 0b1110 and %dirty)
                          } else {
                            %composer.skipToGroupEnd()
                          }
                          %composer.endReplaceableGroup()
                        }, %composer, 0)
                      } else {
                        %composer.skipToGroupEnd()
                      }
                      %composer.endReplaceableGroup()
                    }, %composer, 0)
                  } else {
                    %composer.skipToGroupEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    composeVector(composable, %composer, %changed or 0b0001)
                  }
                }
                @Composable
                @ComposableInferredTarget(scheme = "[0[0]]")
                fun emit(composable: Function2<Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "C(emit)<compos...>:Test.kt#2487m")
                  composable(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )
    }
}
