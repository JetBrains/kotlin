/*
 * Copyright 2023 The Android Open Source Project
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

class FunctionalInterfaceTransformTests(
    useFir: Boolean
) : AbstractControlFlowTransformTests(useFir) {
    @Test
    fun testFunctionalInterfaceWithExtensionReceiverTransformation() {
        verifyComposeIrTransform(
            source = """
                import androidx.compose.runtime.*
                fun interface TestContent {
                    @Composable
                    fun String.Content()
                }
                @Composable
                fun Test(content: TestContent) {
                    with(content) {
                        "".Content()
                    }
                }

                @Composable
                fun CallTest() {
                    Test { this.length }
                }
            """.trimIndent(),
            expectedTransformed = """
            interface TestContent {
              @Composable
              abstract fun String.Content(%composer: Composer?, %changed: Int)
            }
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Test(content: TestContent, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<Conten...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(content)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                with(content) {
                  %this%with.Content(%composer, 0b0110)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(content, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun CallTest(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CallTest)<Test>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Test(<block>{
                  class <no name provided> : TestContent {
                    @Composable
                    override fun Content(%this%Test: String, %composer: Composer?, %changed: Int) {
                      %composer = %composer.startRestartGroup(<>)
                      sourceInformation(%composer, "C(Content):Test.kt")
                      val %dirty = %changed
                      if (%changed and 0b1110 === 0) {
                        %dirty = %dirty or if (%composer.changed(%this%Test)) 0b0100 else 0b0010
                      }
                      if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                        if (isTraceInProgress()) {
                          traceEventStart(<>, %dirty, -1, <>)
                        }
                        %this%Test.length
                        if (isTraceInProgress()) {
                          traceEventEnd()
                        }
                      } else {
                        %composer.skipToGroupEnd()
                      }
                      val tmp0_rcvr = <this>
                      %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                        tmp0_rcvr.Content(%this%Test, %composer, updateChangedFlags(%changed or 0b0001))
                      }
                    }
                  }
                  <no name provided>()
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CallTest(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun testFunInterfaces() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface A {
                fun compute(value: Int): Unit
            }

            @Composable
            fun Example(a: A) {
                Example { it -> a.compute(it) }
            }
        """,
        """
            interface A {
              abstract fun compute(value: Int)
            }
            @Composable
            fun Example(a: A, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)<Exampl...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Example(A { it: Int ->
                  a.compute(it)
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testComposableFunInterfaces() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface A {
                @Composable fun compute(value: Int): Unit
            }
            fun Example(a: A) {
                Example { it -> a.compute(it) }
            }
        """,
        """
            interface A {
              @Composable
              abstract fun compute(value: Int, %composer: Composer?, %changed: Int)
            }
            fun Example(a: A) {
              Example(<block>{
                class <no name provided> : A {
                  @Composable
                  override fun compute(it: Int, %composer: Composer?, %changed: Int) {
                    %composer = %composer.startRestartGroup(<>)
                    sourceInformation(%composer, "C(compute)<comput...>:Test.kt")
                    val %dirty = %changed
                    if (%changed and 0b1110 === 0) {
                      %dirty = %dirty or if (%composer.changed(it)) 0b0100 else 0b0010
                    }
                    if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %dirty, -1, <>)
                      }
                      a.compute(it, %composer, 0b1110 and %dirty)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                    val tmp0_rcvr = <this>
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      tmp0_rcvr.compute(it, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                  }
                }
                <no name provided>()
              })
            }
        """
    )

    @Test
    fun testComposableFunInterfacesInVariance() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer<T> {
                @Composable fun consume(t: T)
            }

            class Repro<T : Any> {
                fun test(consumer: Consumer<in T>) {}
            }

            fun test() {
                Repro<String>().test { string ->
                    println(string)
                }
            }
        """,
        """
            interface Consumer<T>  {
              @Composable
              abstract fun consume(t: T, %composer: Composer?, %changed: Int)
            }
            @StabilityInferred(parameters = 0)
            class Repro<T: Any>  {
              fun test(consumer: Consumer<in T>) { }
              static val %stable: Int = 0
            }
            fun test() {
              Repro().test(<block>{
                class <no name provided> : Consumer<Any?> {
                  @Composable
                  override fun consume(string: String, %composer: Composer?, %changed: Int) {
                    %composer = %composer.startRestartGroup(<>)
                    sourceInformation(%composer, "C(consume):Test.kt")
                    val %dirty = %changed
                    if (%changed and 0b1110 === 0) {
                      %dirty = %dirty or if (%composer.changed(string)) 0b0100 else 0b0010
                    }
                    if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                      if (isTraceInProgress()) {
                        traceEventStart(<>, %dirty, -1, <>)
                      }
                      println(string)
                      if (isTraceInProgress()) {
                        traceEventEnd()
                      }
                    } else {
                      %composer.skipToGroupEnd()
                    }
                    val tmp0_rcvr = <this>
                    %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                      tmp0_rcvr.consume(string, %composer, updateChangedFlags(%changed or 0b0001))
                    }
                  }
                }
                <no name provided>()
              })
            }
        """
    )

    @Test
    fun testCaptureStableFunInterface() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                fun consume(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example {
                    println(int)
                }
            }

            @Composable inline fun Example(consumer: Consumer) {
            }
        """,
        """
            interface Consumer {
              abstract fun consume(t: Int)
            }
            @Composable
            fun Test(int: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<{>,<Exampl...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(int)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                Example(remember(int, {
                  Consumer { it: Int ->
                    println(int)
                  }
                }, %composer, 0b1110 and %dirty), %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(int, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Example(consumer: Consumer, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "CC(Example):Test.kt")
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testNoCaptureFunInterface() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                fun consume(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example {
                    println(it)
                }
            }

            @Composable inline fun Example(consumer: Consumer) {
            }
        """,
        """
            interface Consumer {
              abstract fun consume(t: Int)
            }
            @Composable
            fun Test(int: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Exampl...>:Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Example(Consumer { it: Int ->
                  println(it)
                }, %composer, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(int, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Example(consumer: Consumer, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "CC(Example):Test.kt")
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testComposableFunInterfaceWAnonymousParam() = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.*

            fun interface Consumer {
                @Composable operator fun invoke(t: Int)
            }

            @Composable fun Test(int: Int) {
                Example { _ ->
                }
            }

            @Composable fun Example(consumer: Consumer) {
            }
        """,
        """
            interface Consumer {
              @Composable
              abstract fun invoke(t: Int, %composer: Composer?, %changed: Int)
            }
            @Composable
            fun Test(int: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Exampl...>:Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Example(<block>{
                  class <no name provided> : Consumer {
                    @Composable
                    override fun invoke(<unused var>: Int, %composer: Composer?, %changed: Int) {
                      %composer = %composer.startRestartGroup(<>)
                      sourceInformation(%composer, "C(invoke):Test.kt")
                      if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                        if (isTraceInProgress()) {
                          traceEventStart(<>, %changed, -1, <>)
                        }
                        Unit
                        if (isTraceInProgress()) {
                          traceEventEnd()
                        }
                      } else {
                        %composer.skipToGroupEnd()
                      }
                      val tmp0_rcvr = <this>
                      %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                        tmp0_rcvr.invoke(<unused var>, %composer, updateChangedFlags(%changed or 0b0001))
                      }
                    }
                  }
                  <no name provided>()
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(int, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Example(consumer: Consumer, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(consumer, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )
}
