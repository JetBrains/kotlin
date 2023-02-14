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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.junit.Test

class ContextReceiversTransformTests : AbstractIrTransformTest() {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = languageVersionSettings.languageVersion,
            apiVersion = languageVersionSettings.apiVersion,
            specificFeatures = mapOf(
                LanguageFeature.ContextReceivers to LanguageFeature.State.ENABLED
            )
        )
    }

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
            fun Test(_context_receiver_0: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
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
                Test(_context_receiver_0, %composer, updateChangedFlags(%changed or 0b0001))
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
            fun A(_context_receiver_0: Foo, _context_receiver_1: Bar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
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
                A(_context_receiver_0, _context_receiver_1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun B(_context_receiver_0: Foo, _context_receiver_1: Bar, _context_receiver_2: FooBar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
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
                B(_context_receiver_0, _context_receiver_1, _context_receiver_2, %composer, updateChangedFlags(%changed or 0b0001))
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
            fun String.A(_context_receiver_0: Foo, _context_receiver_1: Bar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
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
                A(_context_receiver_0, _context_receiver_1, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun String.B(_context_receiver_0: Foo, _context_receiver_1: Bar, _context_receiver_2: FooBar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
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
                B(_context_receiver_0, _context_receiver_1, _context_receiver_2, %composer, updateChangedFlags(%changed or 0b0001))
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
            fun A(_context_receiver_0: Foo, _context_receiver_1: Bar, a: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(A):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (%default and 0b0100 !== 0) {
                  a = 1
                }
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
                A(_context_receiver_0, _context_receiver_1, a, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
            @Composable
            fun B(_context_receiver_0: Foo, _context_receiver_1: Bar, _context_receiver_2: FooBar, a: Int, b: String?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(B):Test.kt")
              if (%changed and 0b0001 !== 0 || !%composer.skipping) {
                if (%default and 0b00010000 !== 0) {
                  b = ""
                }
                if (%default and 0b00100000 !== 0) {
                  c = 1
                }
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
                B(_context_receiver_0, _context_receiver_1, _context_receiver_2, a, b, c, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
            @Composable
            fun C(_context_receiver_0: Foo, a: Int, bar: Bar?, %composer: Composer?, %changed: Int, %default: Int) {
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
                C(_context_receiver_0, a, bar, %composer, updateChangedFlags(%changed or 0b0001), %default)
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
        fun String.B(_context_receiver_0: Foo, _context_receiver_1: Bar, _context_receiver_2: FooBar, a: Int, b: String?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(B):Test.kt")
          if (%changed and 0b0001 !== 0 || !%composer.skipping) {
            if (%default and 0b00010000 !== 0) {
              b = ""
            }
            if (%default and 0b00100000 !== 0) {
              c = 1
            }
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
            B(_context_receiver_0, _context_receiver_1, _context_receiver_2, a, b, c, %composer, updateChangedFlags(%changed or 0b0001), %default)
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
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                with(foo) {
                  A(%this%with, %composer, 0)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, updateChangedFlags(%changed or 0b0001))
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
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A()>,<B()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                with(foo) {
                  A(%this%with, %composer, 0)
                  with(Bar()) {
                    B(%this%with, %this%with, %composer, 0)
                  }
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, updateChangedFlags(%changed or 0b0001))
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
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<A(2)>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(foo)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                with(foo) {
                  "Hello".A(%this%with, 2, null, %composer, 0b000110000110, 0b0100)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, %composer, updateChangedFlags(%changed or 0b0001))
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
            fun Test(_context_receiver_0: A, _context_receiver_1: B, _context_receiver_2: C, _context_receiver_3: D, _context_receiver_4: E, _context_receiver_5: F, _context_receiver_6: G, _context_receiver_7: H, _context_receiver_8: I, _context_receiver_9: J, _context_receiver_10: K, _context_receiver_11: L, %composer: Composer?, %changed: Int, %changed1: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(!2,4,5,6,7,8,9,10,11):Test.kt")
              if (%changed and 0b0001 !== 0 || %changed1 and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, %changed1, <>)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(_context_receiver_0, _context_receiver_1, _context_receiver_2, _context_receiver_3, _context_receiver_4, _context_receiver_5, _context_receiver_6, _context_receiver_7, _context_receiver_8, _context_receiver_9, _context_receiver_10, _context_receiver_11, %composer, updateChangedFlags(%changed or 0b0001), updateChangedFlags(%changed1))
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
            fun Test(_context_receiver_0: Foo, a: String, b: Function3<String, Composer, Int, Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<b("yay...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(b)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001010000001 !== 0b10000000 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                b("yay", %composer, 0b0110 or 0b01110000 and %dirty shr 0b0011)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(_context_receiver_0, a, b, %composer, updateChangedFlags(%changed or 0b0001))
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
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Parent)*<Test()>,<Test(a>,<Test(b>,<Test(a>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                with(Foo()) {
                  Test(%this%with, null, 0, %composer, 0, 0b0110)
                  Test(%this%with, "a", 0, %composer, 0b00110000, 0b0100)
                  Test(%this%with, null, 101, %composer, 0b000110000000, 0b0010)
                  Test(%this%with, "Yes", 10, %composer, 0b000110110000, 0)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Parent(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test(_context_receiver_0: Foo, a: String?, b: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(_context_receiver_0)) 0b0100 else 0b0010
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val combineParams = a + b
                if (_context_receiver_0.someString == combineParams) {
                  println("Same same")
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(_context_receiver_0, a, b, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )
}