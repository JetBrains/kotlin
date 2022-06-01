/*
 * Copyright 2022 The Android Open Source Project
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
import org.jetbrains.kotlin.config.LanguageFeature
import org.junit.Test

class ContextReceiversTransformTests : ComposeIrTransformTest() {

    private fun contextReceivers(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
    ) = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.Composable

            $checked
        """.trimIndent(),
        expectedTransformed = expectedTransformed,
        extra = """
            import androidx.compose.runtime.Composable

            $unchecked

            fun used(x: Any?) {}
        """.trimIndent(),
        compilation = JvmCompilation(specificFeature = setOf(LanguageFeature.ContextReceivers))
    )

    @Test
    fun testTrivialContextReceivers(): Unit = contextReceivers(
        """
            class Foo { }
        """,
        """
            context(Foo)
            @Composable
            fun Test() { }
        """,
        """
            @Composable
            fun Test(%this%: Foo, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%this%, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testMultipleContextReceivers(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun A() { }

            context(Foo, Bar, FooBar)
            @Composable
            fun B() { }
        """,
        """
            @Composable
            fun A(%this%: Foo, %this%: Bar, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%this%, %this%, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun B(%this%: Foo, %this%: Bar, %this%: FooBar, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(%this%, %this%, %this%, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiversAndExtensionReceiver(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun String.A() { }

            context(Foo, Bar, FooBar)
            @Composable
            fun String.B() { }
        """,
        """
            @Composable
            fun String.A(%this%: Foo, %this%: Bar, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%this%, %this%, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun String.B(%this%: Foo, %this%: Bar, %this%: FooBar, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(%this%, %this%, %this%, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiversAndDefaultParams(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar)
            @Composable
            fun A(a: Int = 1) { }

            context(Foo, Bar, FooBar)
            @Composable
            fun B(a: Int, b: String = "", c: Int = 1) { }

            context(Foo)
            @Composable
            fun C(a: Int, bar: Bar = Bar()) { }
        """,
        """
            @Composable
            fun A(%this%: Foo, %this%: Bar, a: Int, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (%default and 0b0100 !== 0) {
                  a = 1
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                A(%this%, %this%, a, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun B(%this%: Foo, %this%: Bar, %this%: FooBar, a: Int, b: String?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (%default and 0b00010000 !== 0) {
                  b = ""
                }
                if (%default and 0b00100000 !== 0) {
                  c = 1
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                B(%this%, %this%, %this%, a, b, c, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun C(%this%: Foo, a: Int, bar: Bar?, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(C):Test.kt")
              val %dirty = %changed
              if (%dirty and 0b0001 !== 0 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0100 !== 0) {
                    bar = Bar()
                    %dirty = %dirty and 0b001110000000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0100 !== 0) {
                    %dirty = %dirty and 0b001110000000.inv()
                  }
                }
                %composer.endDefaults()
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                C(%this%, a, bar, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiversAndExtensionReceiverAndDefaultParams(): Unit = contextReceivers(
        """
            class Foo { }
            class Bar { }
            class FooBar { }
        """,
        """
            context(Foo, Bar, FooBar)
            @Composable
            fun String.B(a: Int, b: String = "", c: Int = 1) { }
        """,
        """
        @Composable
        fun String.B(%this%: Foo, %this%: Bar, %this%: FooBar, a: Int, b: String?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
          if (isTraceInProgress()) {
            traceEventStart(<>)
          }
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(B):Test.kt")
          if (%changed and 0b0001 !== 0 || !%composer.skipping) {
            if (%default and 0b00010000 !== 0) {
              b = ""
            }
            if (%default and 0b00100000 !== 0) {
              c = 1
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            B(%this%, %this%, %this%, a, b, c, %composer, %changed or 0b0001, %default)
          }
          if (isTraceInProgress()) {
            traceEventEnd()
          }
        }
        """
    )

    @Test
    fun testContextReceiversWith(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun A() { }

            class Foo { }
        """,
        """

            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                  A()
                }
            }
        """,
        """
            @Composable
            fun Test(foo: Foo, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                with(foo) {
                  A(%this%with, %composer, 0)
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiversNestedWith(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun A() { }

            context(Foo, Bar)
            @Composable
            fun B() { }

            class Foo { }
            class Bar { }
        """,
        """
            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                    A()
                    with(Bar()) {
                        B()
                    }
                }
            }
        """,
        """
            @Composable
            fun Test(foo: Foo, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A()>,<B()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                with(foo) {
                  A(%this%with, %composer, 0)
                  with(Bar()) {
                    B(%this%with, %this%with, %composer, 0)
                  }
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiversWithAndDefaultParam(): Unit = contextReceivers(
        """
            context(Foo)
            @Composable
            fun String.A(param1: Int, param2: String = "") { }

            class Foo { }
        """,
        """
            @Composable
            fun Test(foo: Foo) {
                with(foo) {
                  "Hello".A(2)
                }
            }
        """,
        """
            @Composable
            fun Test(foo: Foo, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A(2)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                with(foo) {
                  "Hello".A(%this%with, 2, null, %composer, 0b000110000110, 0b0100)
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testLotsOfContextReceivers(): Unit = contextReceivers(
        """
            class A { }
            class B { }
            class C { }
            class D { }
            class E { }
            class F { }
            class G { }
            class H { }
            class I { }
            class J { }
            class K { }
            class L { }
        """,
        """
            context(A, B, C, D, E, F, G, H, I, J, K, L)
            @Composable
            fun Test() {
            }
        """,
        """
            @Composable
            fun Test(%this%: A, %this%: B, %this%: C, %this%: D, %this%: E, %this%: F, %this%: G, %this%: H, %this%: I, %this%: J, %this%: K, %this%: L, %composer: Composer?, %changed: Int, %changed1: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              if (%changed and 0b0001 !== 0 || %changed1 and 0b0001 !== 0 || !%composer.skipping) {
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%this%, %this%, %this%, %this%, %this%, %this%, %this%, %this%, %this%, %this%, %this%, %this%, %composer, %changed or 0b0001, %changed1)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiverAndComposableLambdaParam(): Unit = contextReceivers(
        """
            class Foo { }
        """,
        """
            context(Foo)
            @Composable
            fun Test(a: String, b: @Composable (String) -> Unit) {
                b("yay")
            }
        """,
        """
            @Composable
            @ComposableInferredTarget(scheme = "[0[0]]")
            fun Test(%this%: Foo, a: String, b: Function3<String, Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<b("yay...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001010000001 !== 0b10000000 || !%composer.skipping) {
                b("yay", %composer, 0b0110 or 0b01110000 and %dirty shr 0b0011)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%this%, a, b, %composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )

    @Test
    fun testContextReceiverAndDefaultParamsUsage(): Unit = contextReceivers(
        """
            class Foo {
                val someString = "Some String"
            }
        """,
        """
            @Composable
            fun Parent() {
                with(Foo()) {
                    Test()
                    Test(a = "a")
                    Test(b = 101)
                    Test(a = "Yes", b = 10)
                }
            }

            context(Foo)
            @Composable
            fun Test(a: String = "A", b: Int = 2) {
                val combineParams = a + b
                if (someString == combineParams) {
                    println("Same same")
                }
            }
        """,
        """
            @Composable
            fun Parent(%composer: Composer?, %changed: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Parent)*<Test()>,<Test(a>,<Test(b>,<Test(a>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                with(Foo()) {
                  Test(%this%with, null, 0, %composer, 0, 0b0110)
                  Test(%this%with, "a", 0, %composer, 0b00110000, 0b0100)
                  Test(%this%with, null, 101, %composer, 0b000110000000, 0b0010)
                  Test(%this%with, "Yes", 10, %composer, 0b000110110000, 0)
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Parent(%composer, %changed or 0b0001)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
            @Composable
            fun Test(%this%: Foo, a: String?, b: Int, %composer: Composer?, %changed: Int, %default: Int) {
              if (isTraceInProgress()) {
                traceEventStart(<>)
              }
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(%this%)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                if (%default and 0b0010 !== 0) {
                  a = "A"
                }
                if (%default and 0b0100 !== 0) {
                  b = 2
                }
                val combineParams = a + b
                if (%this%.someString == combineParams) {
                  println("Same same")
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%this%, a, b, %composer, %changed or 0b0001, %default)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            }
        """
    )
}