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
import org.junit.Test

class DefaultParamTransformTests : ComposeIrTransformTest() {
    private fun defaultParams(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            $unchecked

            fun used(x: Any?) {}
        """.trimIndent(),
        dumpTree = dumpTree
    )

    @Test
    fun testComposableWithAndWithoutDefaultParams(): Unit = defaultParams(
        """
            @Composable fun A(x: Int) { }
            @Composable fun B(x: Int = 1) { }
        """,
        """
            @Composable
            fun Test() {
                A(1)
                B()
                B(2)
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(1)>,<B()>,<B(2)>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                A(1, %composer, 0b0110)
                B(0, %composer, 0, 0b0001)
                B(2, %composer, 0b0110, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testInlineClassDefaultParameter(): Unit = defaultParams(
        """
            inline class Foo(val value: Int)
        """,
        """
            @Composable
            fun Example(foo: Foo = Foo(0)) {
                print(foo)
            }
            @Composable
            fun Test() {
                Example()
            }
        """,
        """
            @Composable
            fun Example(foo: Foo, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example)P(0:Foo):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(foo))) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  foo = Foo(0)
                }
                print(foo)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(foo, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<Exampl...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                Example(<unsafe-coerce>(0), %composer, 0, 0b0001)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testParameterHoles(): Unit = defaultParams(
        """
            @Composable fun A(a: Int = 0, b: Int = 1, c: Int = 2, d: Int = 3, e: Int = 4) { }
        """,
        """
            @Composable
            fun Test() {
                A(0, 1, 2)
                A(a = 0, c = 2)
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A(0,>,<A(a>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                A(0, 1, 2, 0, 0, %composer, 0b000110110110, 0b00011000)
                A(0, 0, 2, 0, 0, %composer, 0b000110000110, 0b00011010)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testUnusedDefaultComposableLambda(): Unit = defaultParams(
        """
        """,
        """
            inline fun Bar(unused: @Composable () -> Unit = { }) {}
            fun Foo() { Bar() }
        """,
        """
            fun Bar(unused: Function2<Composer, Int, Unit> = { %composer: Composer?, %changed: Int ->
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C:Test.kt")
              if (%changed and 0b1011 !== 0b0010 || !%composer.skipping) {
                Unit
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endReplaceableGroup()
            }
            ) { }
            fun Foo() {
              Bar()
            }
        """
    )

    @Test
    fun testNonStaticDefaultExpressions(): Unit = defaultParams(
        """
            fun makeInt(): Int = 123
        """,
        """
            @Composable
            fun Test(x: Int = makeInt()) {
                used(x)
            }
        """,
        """
            @Composable
            fun Test(x: Int, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    x = makeInt()
                    %dirty = %dirty and 0b1110.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                %composer.endDefaults()
                used(x)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testEarlierParameterReferences(): Unit = defaultParams(
        """
        """,
        """
            @Composable
            fun A(a: Int = 0, b: Int = a + 1) {
                print(a)
                print(b)
            }
        """,
        """
            @Composable
            fun A(a: Int, b: Int, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    b = a + 1
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                print(a)
                print(b)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(a, b, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun test30Parameters(): Unit = defaultParams(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0
            ) {
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, %composer: Composer?, %changed: Int, %changed1: Int, %changed2: Int, %changed3: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val %dirty3 = %changed3
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b001110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b110000000000000000000000
              } else if (%changed and 0b0001110000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b1110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000000000000000
              } else if (%changed and 0b01110000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b1110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a10)) 0b0100 else 0b0010
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000
              } else if (%changed1 and 0b01110000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a11)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b001110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a12)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000
              } else if (%changed1 and 0b0001110000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a13)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b1110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a14)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000
              } else if (%changed1 and 0b01110000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b001110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000000000000000
              } else if (%changed1 and 0b0001110000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b1110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000000000000000
              } else if (%changed1 and 0b01110000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b1110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a20)) 0b0100 else 0b0010
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000
              } else if (%changed2 and 0b01110000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a21)) 0b00100000 else 0b00010000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000
              } else if (%changed2 and 0b001110000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a22)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000
              } else if (%changed2 and 0b0001110000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a23)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000
              } else if (%changed2 and 0b1110000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a24)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000
              } else if (%changed2 and 0b01110000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a25)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000000000000000
              } else if (%changed2 and 0b001110000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a26)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000000000000000
              } else if (%changed2 and 0b0001110000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a27)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000000000000000
              } else if (%changed2 and 0b1110000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a28)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000000000000000
              } else if (%changed2 and 0b01110000000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a29)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty3 = %dirty3 or 0b0110
              } else if (%changed3 and 0b1110 === 0) {
                %dirty3 = %dirty3 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%dirty and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty1 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty2 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty3 and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                if (%default and 0b1000000000000000 !== 0) {
                  a15 = 0
                }
                if (%default and 0b00010000000000000000 !== 0) {
                  a16 = 0
                }
                if (%default and 0b00100000000000000000 !== 0) {
                  a17 = 0
                }
                if (%default and 0b01000000000000000000 !== 0) {
                  a18 = 0
                }
                if (%default and 0b10000000000000000000 !== 0) {
                  a19 = 0
                }
                if (%default and 0b000100000000000000000000 !== 0) {
                  a20 = 0
                }
                if (%default and 0b001000000000000000000000 !== 0) {
                  a21 = 0
                }
                if (%default and 0b010000000000000000000000 !== 0) {
                  a22 = 0
                }
                if (%default and 0b100000000000000000000000 !== 0) {
                  a23 = 0
                }
                if (%default and 0b0001000000000000000000000000 !== 0) {
                  a24 = 0
                }
                if (%default and 0b0010000000000000000000000000 !== 0) {
                  a25 = 0
                }
                if (%default and 0b0100000000000000000000000000 !== 0) {
                  a26 = 0
                }
                if (%default and 0b1000000000000000000000000000 !== 0) {
                  a27 = 0
                }
                if (%default and 0b00010000000000000000000000000000 !== 0) {
                  a28 = 0
                }
                if (%default and 0b00100000000000000000000000000000 !== 0) {
                  a29 = 0
                }
                if (%default and 0b01000000000000000000000000000000 !== 0) {
                  a30 = 0
                }
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, %composer, %changed or 0b0001, %changed1, %changed2, %changed3, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun test31Parameters(): Unit = defaultParams(
        """
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Int = 0,
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0,
                a31: Int = 0
            ) {
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
                used(a31)
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, a31: Int, %composer: Composer?, %changed: Int, %changed1: Int, %changed2: Int, %changed3: Int, %default: Int, %default1: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val %dirty3 = %changed3
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b001110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b110000000000000000000000
              } else if (%changed and 0b0001110000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b1110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b001000000000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000000000000000
              } else if (%changed and 0b01110000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a09)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b1110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a10)) 0b0100 else 0b0010
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000
              } else if (%changed1 and 0b01110000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a11)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b001110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a12)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000
              } else if (%changed1 and 0b0001110000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a13)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b1110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a14)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000
              } else if (%changed1 and 0b01110000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b001110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000000000000000
              } else if (%changed1 and 0b0001110000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b1110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000000000000000
              } else if (%changed1 and 0b01110000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b1110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a20)) 0b0100 else 0b0010
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000
              } else if (%changed2 and 0b01110000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a21)) 0b00100000 else 0b00010000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000
              } else if (%changed2 and 0b001110000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a22)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000
              } else if (%changed2 and 0b0001110000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a23)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000
              } else if (%changed2 and 0b1110000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a24)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000
              } else if (%changed2 and 0b01110000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a25)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000000000000000
              } else if (%changed2 and 0b001110000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a26)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000000000000000
              } else if (%changed2 and 0b0001110000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a27)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000000000000000
              } else if (%changed2 and 0b1110000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a28)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000000000000000
              } else if (%changed2 and 0b01110000000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a29)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty3 = %dirty3 or 0b0110
              } else if (%changed3 and 0b1110 === 0) {
                %dirty3 = %dirty3 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%default1 and 0b0001 !== 0) {
                %dirty3 = %dirty3 or 0b00110000
              } else if (%changed3 and 0b01110000 === 0) {
                %dirty3 = %dirty3 or if (%composer.changed(a31)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty1 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty2 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty3 and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a00 = 0
                }
                if (%default and 0b0010 !== 0) {
                  a01 = 0
                }
                if (%default and 0b0100 !== 0) {
                  a02 = 0
                }
                if (%default and 0b1000 !== 0) {
                  a03 = 0
                }
                if (%default and 0b00010000 !== 0) {
                  a04 = 0
                }
                if (%default and 0b00100000 !== 0) {
                  a05 = 0
                }
                if (%default and 0b01000000 !== 0) {
                  a06 = 0
                }
                if (%default and 0b10000000 !== 0) {
                  a07 = 0
                }
                if (%default and 0b000100000000 !== 0) {
                  a08 = 0
                }
                if (%default and 0b001000000000 !== 0) {
                  a09 = 0
                }
                if (%default and 0b010000000000 !== 0) {
                  a10 = 0
                }
                if (%default and 0b100000000000 !== 0) {
                  a11 = 0
                }
                if (%default and 0b0001000000000000 !== 0) {
                  a12 = 0
                }
                if (%default and 0b0010000000000000 !== 0) {
                  a13 = 0
                }
                if (%default and 0b0100000000000000 !== 0) {
                  a14 = 0
                }
                if (%default and 0b1000000000000000 !== 0) {
                  a15 = 0
                }
                if (%default and 0b00010000000000000000 !== 0) {
                  a16 = 0
                }
                if (%default and 0b00100000000000000000 !== 0) {
                  a17 = 0
                }
                if (%default and 0b01000000000000000000 !== 0) {
                  a18 = 0
                }
                if (%default and 0b10000000000000000000 !== 0) {
                  a19 = 0
                }
                if (%default and 0b000100000000000000000000 !== 0) {
                  a20 = 0
                }
                if (%default and 0b001000000000000000000000 !== 0) {
                  a21 = 0
                }
                if (%default and 0b010000000000000000000000 !== 0) {
                  a22 = 0
                }
                if (%default and 0b100000000000000000000000 !== 0) {
                  a23 = 0
                }
                if (%default and 0b0001000000000000000000000000 !== 0) {
                  a24 = 0
                }
                if (%default and 0b0010000000000000000000000000 !== 0) {
                  a25 = 0
                }
                if (%default and 0b0100000000000000000000000000 !== 0) {
                  a26 = 0
                }
                if (%default and 0b1000000000000000000000000000 !== 0) {
                  a27 = 0
                }
                if (%default and 0b00010000000000000000000000000000 !== 0) {
                  a28 = 0
                }
                if (%default and 0b00100000000000000000000000000000 !== 0) {
                  a29 = 0
                }
                if (%default and 0b01000000000000000000000000000000 !== 0) {
                  a30 = 0
                }
                if (%default1 and 0b0001 !== 0) {
                  a31 = 0
                }
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
                used(a31)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, %composer, %changed or 0b0001, %changed1, %changed2, %changed3, %default, %default1)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun test31ParametersWithSomeUnstable(): Unit = defaultParams(
        """
            class Foo
        """,
        """
            @Composable
            fun Example(
                a00: Int = 0,
                a01: Int = 0,
                a02: Int = 0,
                a03: Int = 0,
                a04: Int = 0,
                a05: Int = 0,
                a06: Int = 0,
                a07: Int = 0,
                a08: Int = 0,
                a09: Foo = Foo(),
                a10: Int = 0,
                a11: Int = 0,
                a12: Int = 0,
                a13: Int = 0,
                a14: Int = 0,
                a15: Int = 0,
                a16: Int = 0,
                a17: Int = 0,
                a18: Int = 0,
                a19: Int = 0,
                a20: Int = 0,
                a21: Int = 0,
                a22: Int = 0,
                a23: Int = 0,
                a24: Int = 0,
                a25: Int = 0,
                a26: Int = 0,
                a27: Int = 0,
                a28: Int = 0,
                a29: Int = 0,
                a30: Int = 0,
                a31: Foo = Foo()
            ) {
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
                used(a31)
            }
        """,
        """
            @Composable
            fun Example(a00: Int, a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Foo?, a10: Int, a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int, a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int, a31: Foo?, %composer: Composer?, %changed: Int, %changed1: Int, %changed2: Int, %changed3: Int, %default: Int, %default1: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Example):Test.kt")
              val %dirty = %changed
              val %dirty1 = %changed1
              val %dirty2 = %changed2
              val %dirty3 = %changed3
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a00)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a01)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a02)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b1000 !== 0) {
                %dirty = %dirty or 0b110000000000
              } else if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a03)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b00010000 !== 0) {
                %dirty = %dirty or 0b0110000000000000
              } else if (%changed and 0b1110000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a04)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b00100000 !== 0) {
                %dirty = %dirty or 0b00110000000000000000
              } else if (%changed and 0b01110000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a05)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b01000000 !== 0) {
                %dirty = %dirty or 0b000110000000000000000000
              } else if (%changed and 0b001110000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a06)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b10000000 !== 0) {
                %dirty = %dirty or 0b110000000000000000000000
              } else if (%changed and 0b0001110000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a07)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b000100000000 !== 0) {
                %dirty = %dirty or 0b0110000000000000000000000000
              } else if (%changed and 0b1110000000000000000000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(a08)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%changed and 0b01110000000000000000000000000000 === 0) {
                %dirty = %dirty or if (%default and 0b001000000000 === 0 && %composer.changed(a09)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b010000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110
              } else if (%changed1 and 0b1110 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a10)) 0b0100 else 0b0010
              }
              if (%default and 0b100000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000
              } else if (%changed1 and 0b01110000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a11)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0001000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000
              } else if (%changed1 and 0b001110000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a12)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b0010000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000
              } else if (%changed1 and 0b0001110000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a13)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0100000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000
              } else if (%changed1 and 0b1110000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a14)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b1000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000
              } else if (%changed1 and 0b01110000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a15)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b00010000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b000110000000000000000000
              } else if (%changed1 and 0b001110000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a16)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b00100000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b110000000000000000000000
              } else if (%changed1 and 0b0001110000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a17)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b01000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b0110000000000000000000000000
              } else if (%changed1 and 0b1110000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a18)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b10000000000000000000 !== 0) {
                %dirty1 = %dirty1 or 0b00110000000000000000000000000000
              } else if (%changed1 and 0b01110000000000000000000000000000 === 0) {
                %dirty1 = %dirty1 or if (%composer.changed(a19)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b000100000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110
              } else if (%changed2 and 0b1110 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a20)) 0b0100 else 0b0010
              }
              if (%default and 0b001000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000
              } else if (%changed2 and 0b01110000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a21)) 0b00100000 else 0b00010000
              }
              if (%default and 0b010000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000
              } else if (%changed2 and 0b001110000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a22)) 0b000100000000 else 0b10000000
              }
              if (%default and 0b100000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000
              } else if (%changed2 and 0b0001110000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a23)) 0b100000000000 else 0b010000000000
              }
              if (%default and 0b0001000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000
              } else if (%changed2 and 0b1110000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a24)) 0b0100000000000000 else 0b0010000000000000
              }
              if (%default and 0b0010000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000
              } else if (%changed2 and 0b01110000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a25)) 0b00100000000000000000 else 0b00010000000000000000
              }
              if (%default and 0b0100000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b000110000000000000000000
              } else if (%changed2 and 0b001110000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a26)) 0b000100000000000000000000 else 0b10000000000000000000
              }
              if (%default and 0b1000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b110000000000000000000000
              } else if (%changed2 and 0b0001110000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a27)) 0b100000000000000000000000 else 0b010000000000000000000000
              }
              if (%default and 0b00010000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b0110000000000000000000000000
              } else if (%changed2 and 0b1110000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a28)) 0b0100000000000000000000000000 else 0b0010000000000000000000000000
              }
              if (%default and 0b00100000000000000000000000000000 !== 0) {
                %dirty2 = %dirty2 or 0b00110000000000000000000000000000
              } else if (%changed2 and 0b01110000000000000000000000000000 === 0) {
                %dirty2 = %dirty2 or if (%composer.changed(a29)) 0b00100000000000000000000000000000 else 0b00010000000000000000000000000000
              }
              if (%default and 0b01000000000000000000000000000000 !== 0) {
                %dirty3 = %dirty3 or 0b0110
              } else if (%changed3 and 0b1110 === 0) {
                %dirty3 = %dirty3 or if (%composer.changed(a30)) 0b0100 else 0b0010
              }
              if (%changed3 and 0b01110000 === 0) {
                %dirty3 = %dirty3 or if (%default1 and 0b0001 === 0 && %composer.changed(a31)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty1 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty2 and 0b01011011011011011011011011011011 !== 0b00010010010010010010010010010010 || %dirty3 and 0b01011011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a00 = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    a01 = 0
                  }
                  if (%default and 0b0100 !== 0) {
                    a02 = 0
                  }
                  if (%default and 0b1000 !== 0) {
                    a03 = 0
                  }
                  if (%default and 0b00010000 !== 0) {
                    a04 = 0
                  }
                  if (%default and 0b00100000 !== 0) {
                    a05 = 0
                  }
                  if (%default and 0b01000000 !== 0) {
                    a06 = 0
                  }
                  if (%default and 0b10000000 !== 0) {
                    a07 = 0
                  }
                  if (%default and 0b000100000000 !== 0) {
                    a08 = 0
                  }
                  if (%default and 0b001000000000 !== 0) {
                    a09 = Foo()
                    %dirty = %dirty and 0b01110000000000000000000000000000.inv()
                  }
                  if (%default and 0b010000000000 !== 0) {
                    a10 = 0
                  }
                  if (%default and 0b100000000000 !== 0) {
                    a11 = 0
                  }
                  if (%default and 0b0001000000000000 !== 0) {
                    a12 = 0
                  }
                  if (%default and 0b0010000000000000 !== 0) {
                    a13 = 0
                  }
                  if (%default and 0b0100000000000000 !== 0) {
                    a14 = 0
                  }
                  if (%default and 0b1000000000000000 !== 0) {
                    a15 = 0
                  }
                  if (%default and 0b00010000000000000000 !== 0) {
                    a16 = 0
                  }
                  if (%default and 0b00100000000000000000 !== 0) {
                    a17 = 0
                  }
                  if (%default and 0b01000000000000000000 !== 0) {
                    a18 = 0
                  }
                  if (%default and 0b10000000000000000000 !== 0) {
                    a19 = 0
                  }
                  if (%default and 0b000100000000000000000000 !== 0) {
                    a20 = 0
                  }
                  if (%default and 0b001000000000000000000000 !== 0) {
                    a21 = 0
                  }
                  if (%default and 0b010000000000000000000000 !== 0) {
                    a22 = 0
                  }
                  if (%default and 0b100000000000000000000000 !== 0) {
                    a23 = 0
                  }
                  if (%default and 0b0001000000000000000000000000 !== 0) {
                    a24 = 0
                  }
                  if (%default and 0b0010000000000000000000000000 !== 0) {
                    a25 = 0
                  }
                  if (%default and 0b0100000000000000000000000000 !== 0) {
                    a26 = 0
                  }
                  if (%default and 0b1000000000000000000000000000 !== 0) {
                    a27 = 0
                  }
                  if (%default and 0b00010000000000000000000000000000 !== 0) {
                    a28 = 0
                  }
                  if (%default and 0b00100000000000000000000000000000 !== 0) {
                    a29 = 0
                  }
                  if (%default and 0b01000000000000000000000000000000 !== 0) {
                    a30 = 0
                  }
                  if (%default1 and 0b0001 !== 0) {
                    a31 = Foo()
                    %dirty3 = %dirty3 and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b001000000000 !== 0) {
                    %dirty = %dirty and 0b01110000000000000000000000000000.inv()
                  }
                  if (%default1 and 0b0001 !== 0) {
                    %dirty3 = %dirty3 and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                used(a00)
                used(a01)
                used(a02)
                used(a03)
                used(a04)
                used(a05)
                used(a06)
                used(a07)
                used(a08)
                used(a09)
                used(a10)
                used(a11)
                used(a12)
                used(a13)
                used(a14)
                used(a15)
                used(a16)
                used(a17)
                used(a18)
                used(a19)
                used(a20)
                used(a21)
                used(a22)
                used(a23)
                used(a24)
                used(a25)
                used(a26)
                used(a27)
                used(a28)
                used(a29)
                used(a30)
                used(a31)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Example(a00, a01, a02, a03, a04, a05, a06, a07, a08, a09, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, %composer, %changed or 0b0001, %changed1, %changed2, %changed3, %default, %default1)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testDefaultArgsForFakeOverridesSuperMethods(): Unit = defaultParams(
        """
        """,
        """
            open class Foo {
                @NonRestartableComposable @Composable fun foo(x: Int = 0) {}
            }
            class Bar: Foo() {
                @NonRestartableComposable @Composable fun Example() {
                    foo()
                }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            open class Foo {
              @NonRestartableComposable
              @Composable
              fun foo(x: Int, %composer: Composer?, %changed: Int, %default: Int) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(foo):Test.kt")
                if (%default and 0b0001 !== 0) {
                  x = 0
                }
                %composer.endReplaceableGroup()
              }
              static val %stable: Int = 0
            }
            @StabilityInferred(parameters = 0)
            class Bar : Foo {
              @NonRestartableComposable
              @Composable
              fun Example(%composer: Composer?, %changed: Int) {
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "C(Example)<foo()>:Test.kt")
                foo(0, %composer, 0b01110000 and %changed shl 0b0011, 0b0001)
                %composer.endReplaceableGroup()
              }
              static val %stable: Int = 0
            }
        """
    )

    @Test
    fun testDefaultArgsOnInvoke() = defaultParams(
        """
            object HasDefault {
                @Composable
                operator fun invoke(text: String = "SomeText"){
                    println(text)
                }
            }

            object NoDefault {
                @Composable
                operator fun invoke(text: String){
                    println(text)
                }
            }

            object MultipleDefault {
                @Composable
                operator fun invoke(text: String = "SomeText", value: Int = 5){
                    println(text)
                    println(value)
                }
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Bar() {
                HasDefault()
                NoDefault("Some Text")
                MultipleDefault()
            }
        """,
        """
            @NonRestartableComposable
            @Composable
            fun Bar(%composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Bar)<HasDef...>,<NoDefa...>,<Multip...>:Test.kt")
              HasDefault(null, %composer, 0b00110000, 0b0001)
              NoDefault("Some Text", %composer, 0b00110110)
              MultipleDefault(null, 0, %composer, 0b000110000000, 0b0011)
              %composer.endReplaceableGroup()
            }

        """
        )
}
