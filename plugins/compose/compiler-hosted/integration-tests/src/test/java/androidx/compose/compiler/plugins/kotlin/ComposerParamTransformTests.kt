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

import org.junit.Test

class ComposerParamTransformTests : ComposeIrTransformTest() {
    private fun composerParam(
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            @file:OptIn(
              ExperimentalComposeApi::class,
              InternalComposeApi::class,
              ComposeCompilerApi::class
            )
            package test

            import androidx.compose.runtime.ExperimentalComposeApi
            import androidx.compose.runtime.InternalComposeApi
            import androidx.compose.runtime.ComposeCompilerApi
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.ComposableContract

            $source
        """.trimIndent(),
        expectedTransformed,
        "",
        dumpTree
    )

    @Test
    fun testCallingProperties(): Unit = composerParam(
        """
            @Composable val bar: Int get() { return 123 }

            @ComposableContract(restartable = false) @Composable fun Example() {
                bar
            }
        """,
        """
            val bar: Int
              get() {
                %composer.startReplaceableGroup(<>, "C:Test.kt#2487m")
                val tmp0 = 123
                %composer.endReplaceableGroup()
                return tmp0
              }
            @ComposableContract(restartable = false)
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Example)<bar>:Test.kt#2487m")
              bar
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testAbstractComposable(): Unit = composerParam(
        """
            abstract class BaseFoo {
                @ComposableContract(restartable = false)
                @Composable
                abstract fun bar()
            }

            class FooImpl : BaseFoo() {
                @ComposableContract(restartable = false)
                @Composable
                override fun bar() {}
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            abstract class BaseFoo {
              @ComposableContract(restartable = false)
              @Composable
              abstract fun bar(%composer: Composer<*>?, %changed: Int)
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class FooImpl : BaseFoo {
              @ComposableContract(restartable = false)
              @Composable
              override fun bar(%composer: Composer<*>?, %changed: Int) {
                %composer.startReplaceableGroup(<>, "C(bar):Test.kt#2487m")
                %composer.endReplaceableGroup()
              }
              static val %stable: Int = 0
            }
        """
    )

    @Test
    fun testLocalClassAndObjectLiterals(): Unit = composerParam(
        """
            @ComposableContract(restartable = false)
            @Composable
            fun Wat() {}

            @ComposableContract(restartable = false)
            @Composable
            fun Foo(x: Int) {
                Wat()
                @ComposableContract(restartable = false)
                @Composable fun goo() { Wat() }
                class Bar {
                    @ComposableContract(restartable = false)
                    @Composable fun baz() { Wat() }
                }
                goo()
                Bar().baz()
            }
        """,
        """
            @ComposableContract(restartable = false)
            @Composable
            fun Wat(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Wat):Test.kt#2487m")
              %composer.endReplaceableGroup()
            }
            @ComposableContract(restartable = false)
            @Composable
            fun Foo(x: Int, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Foo)<Wat()>,<goo()>,<baz()>:Test.kt#2487m")
              Wat(%composer, 0)
              @ComposableContract(restartable = false)
              @Composable
              fun goo(%composer: Composer<*>?, %changed: Int) {
                %composer.startReplaceableGroup(<>, "C(goo)<Wat()>:Test.kt#2487m")
                Wat(%composer, 0)
                %composer.endReplaceableGroup()
              }
              class Bar {
                @ComposableContract(restartable = false)
                @Composable
                fun baz(%composer: Composer<*>?, %changed: Int) {
                  %composer.startReplaceableGroup(<>, "C(baz)<Wat()>:Test.kt#2487m")
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
            @ComposableContract(restartable = false)
            @Composable fun Example() {
                Example()
            }
        """,
        """
            @ComposableContract(restartable = false)
            @Composable
            fun Example(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Example)<Exampl...>:Test.kt#2487m")
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

            @ComposableContract(restartable = false)
            @Composable fun Test() {
                Example {}
            }
        """,
        """
            @Composable
            fun Example(content: Function2<Composer<*>, Int, Unit>, %composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Example)<conten...>:Test.kt#2487m")
              content(%composer, 0b1110 and %changed)
              %composer.endReplaceableGroup()
            }
            @ComposableContract(restartable = false)
            @Composable
            fun Test(%composer: Composer<*>?, %changed: Int) {
              %composer.startReplaceableGroup(<>, "C(Test)<Exampl...>:Test.kt#2487m")
              Example({ %composer: Composer<*>?, %changed: Int ->
                %composer.startReplaceableGroup(<>, "C:Test.kt#2487m")
                if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
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
            @Composable
            val myProperty: () -> Unit get() {
                return {  }
            }
        """,
        """
            val myProperty: Function0<Unit>
              get() {
                %composer.startReplaceableGroup(<>, "C:Test.kt#2487m")
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
                fun composeVector(composable: Function2<Composer<*>, Int, Unit>, %composer: Composer<*>?, %changed: Int) {
                  %composer.startRestartGroup(<>, "C(composeVector)<emit>:Test.kt#2487m")
                  val %dirty = %changed
                  if (%changed and 0b1110 === 0) {
                    %dirty = %dirty or if (%composer.changed(composable)) 0b0100 else 0b0010
                  }
                  if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                    emit({ %composer: Composer<*>?, %changed: Int ->
                      %composer.startReplaceableGroup(<>, "C<emit>:Test.kt#2487m")
                      if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                        emit({ %composer: Composer<*>?, %changed: Int ->
                          %composer.startReplaceableGroup(<>, "C<compos...>:Test.kt#2487m")
                          if (%changed and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
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
                  %composer.endRestartGroup()?.updateScope { %composer: Composer<*>?, %force: Int ->
                    composeVector(composable, %composer, %changed or 0b0001)
                  }
                }
                @Composable
                fun emit(composable: Function2<Composer<*>, Int, Unit>, %composer: Composer<*>?, %changed: Int) {
                  %composer.startReplaceableGroup(<>, "C(emit)<compos...>:Test.kt#2487m")
                  composable(%composer, 0b1110 and %changed)
                  %composer.endReplaceableGroup()
                }
            """.trimIndent()
        )
    }
}
