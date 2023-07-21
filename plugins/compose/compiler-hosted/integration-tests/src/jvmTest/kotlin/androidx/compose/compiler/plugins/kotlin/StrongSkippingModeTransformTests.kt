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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class StrongSkippingModeTransformTests(useFir: Boolean) :
    FunctionBodySkippingTransformTestsBase(useFir) {

    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY, false)
        put(ComposeConfiguration.STRONG_SKIPPING_ENABLED_KEY, true)
    }

    @Test
    fun testSingleStableParam(): Unit = comparisonPropagation(
        """
            class Foo(val value: Int = 0)
            @Composable fun A(x: Foo) {}
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(x, %composer, 0b1110 and %dirty)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testSingleUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(x, %composer, 0b1110 and %dirty)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testSingleNullableUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo?) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo?) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo?, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(x, %composer, 0b1110 and %dirty)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testSingleOptionalUnstableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo?) {}
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun Test(x: Foo? = Foo()) {
                A(x)
            }
        """,
        """
            @Composable
            fun Test(x: Foo?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changedInstance(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    x = Foo()
                    %dirty = %dirty and 0b1110.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                %composer.endDefaults()
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(x, %composer, 0b1110 and %dirty)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(x, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testRuntimeStableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            class Holder<T> {
                @Composable
                fun Test(x: T) {
                    A(x as Int)
                }
            }
        """,
        """
            @StabilityInferred(parameters = 0)
            class Holder<T>  {
              @Composable
              fun Test(x: T, %composer: Composer?, %changed: Int) {
                %composer = %composer.startRestartGroup(<>)
                sourceInformation(%composer, "C(Test)")
                val %dirty = %changed
                if (%changed and 0b0110 === 0) {
                  %dirty = %dirty or if (if (%changed and 0b1000 === 0) {
                    %composer.changed(x)
                  } else {
                    %composer.changedInstance(x)
                  }
                  ) 0b0100 else 0b0010
                }
                if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                  if (isTraceInProgress()) {
                    traceEventStart(<>, %dirty, -1, <>)
                  }
                  A(x, %composer, 0)
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                } else {
                  %composer.skipToGroupEnd()
                }
                val tmp0_rcvr = <this>
                %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                  tmp0_rcvr.Test(x, %composer, updateChangedFlags(%changed or 0b0001))
                }
              }
              static val %stable: Int = 0
            }
        """
    )

    @Test
    fun testStableUnstableParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """,
        """
            @Composable fun CanSkip(a: Int = 0, b: Foo = Foo()) {
                used(a)
                used(b)
            }
            @Composable fun CannotSkip(a: Int, b: Foo) {
                used(a)
                used(b)
                print("Hello World")
            }
            @Composable fun NoParams() {
                print("Hello World")
            }
        """,
        """
            @Composable
            fun CanSkip(a: Int, b: Foo?, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CanSkip)")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b00110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changedInstance(b)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b00010011 !== 0b00010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = 0
                  }
                  if (%default and 0b0010 !== 0) {
                    b = Foo()
                    %dirty = %dirty and 0b01110000.inv()
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(a)
                used(b)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CanSkip(a, b, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
            @Composable
            fun CannotSkip(a: Int, b: Foo, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(CannotSkip)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b00110000 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(b)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b00010011 !== 0b00010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(a)
                used(b)
                print("Hello World")
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                CannotSkip(a, b, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun NoParams(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(NoParams)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                print("Hello World")
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                NoParams(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testStaticDefaultParam() = comparisonPropagation(
        """
            @Composable
            fun A(i: Int, list: List<Int>? = null, set: Set<Int> = emptySet()) {}
        """.trimIndent(),
        """
            @Composable
            fun Test(i: Int) {
                A(i)
            }
        """.trimIndent(),
        """
            @Composable
            fun Test(i: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changed(i)) 0b0100 else 0b0010
              }
              if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                A(i, null, null, %composer, 0b1110 and %dirty, 0b0110)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(i, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent()
    )

    @Test
    fun testMemoizingUnstableCapturesInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test() {
                val foo = Foo(0)
                val lambda = { foo }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = Foo(0)
                val lambda = <block>{
                  %composer.startReplaceableGroup(<>)
                  val tmpCache = %composer.cache(%composer.changedInstance(foo)) {
                    {
                      foo
                    }
                  }
                  %composer.endReplaceableGroup()
                  tmpCache
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testDontMemoizeLambdasInMarkedFunction() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
        """.trimIndent(),
        """
            import androidx.compose.runtime.DontMemoize

            @Composable
            @DontMemoize
            fun Test() {
                val foo = Foo(0)
                val lambda = { foo }
            }
        """,
        """
            @Composable
            @DontMemoize
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = Foo(0)
                val lambda = {
                  foo
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testDontMemoizeLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            fun Lam(x: ()->Unit) { x() }
        """.trimIndent(),
        """
            import androidx.compose.runtime.DontMemoize

            @Composable
            fun Test() {
                val foo = Foo(0)
                val lambda = @DontMemoize { foo }
                Lam @DontMemoize { foo }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = Foo(0)
                val lambda = {
                  foo
                }
                Lam {
                  foo
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testMemoizingUnstableFunctionParameterInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test(foo: Foo, bar: Bar) {
                val lambda: ()->Unit = { 
                    foo
                    bar
                }
            }
        """,
        """
            @Composable
            fun Test(foo: Foo, bar: Bar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(1)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(foo)) 0b0100 else 0b0010
              }
              if (%changed and 0b00110000 === 0) {
                %dirty = %dirty or if (%composer.changed(bar)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b00010011 !== 0b00010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val lambda = <block>{
                  %composer.startReplaceableGroup(<>)
                  val tmpCache = %composer.cache(%composer.changedInstance(foo) or %composer.changed(bar)) {
                    {
                      foo
                      bar
                    }
                  }
                  %composer.endReplaceableGroup()
                  tmpCache
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, bar, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent()
    )

    @Test
    fun testMemoizingComposableLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test(foo: Foo, bar: Bar) {
                val lambda: @Composable ()->Unit = {
                    foo
                    bar
                }
            }
        """,
        """
            @Composable
            fun Test(foo: Foo, bar: Bar, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(1)")
              val %dirty = %changed
              if (%changed and 0b0110 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(foo)) 0b0100 else 0b0010
              }
              if (%changed and 0b00110000 === 0) {
                %dirty = %dirty or if (%composer.changed(bar)) 0b00100000 else 0b00010000
              }
              if (%dirty and 0b00010011 !== 0b00010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val lambda = composableLambda(%composer, <>, true) { %composer: Composer?, %changed: Int ->
                  if (%changed and 0b0011 !== 0b0010 || !%composer.skipping) {
                    if (isTraceInProgress()) {
                      traceEventStart(<>, %changed, -1, <>)
                    }
                    foo
                    bar
                    if (isTraceInProgress()) {
                      traceEventEnd()
                    }
                  } else {
                    %composer.skipToGroupEnd()
                  }
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(foo, bar, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent()
    )

    @Test
    fun testMemoizingStableAndUnstableCapturesInLambda() = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
            class Foo(var value: Int = 0)
            class Bar(val value: Int = 0)
        """.trimIndent(),
        """
            @Composable
            fun Test() {
                val foo = Foo(0)
                val bar = Bar(1)
                val lambda = {
                    foo
                    bar
                }
            }
        """,
        """
        @Composable
        fun Test(%composer: Composer?, %changed: Int) {
          %composer = %composer.startRestartGroup(<>)
          sourceInformation(%composer, "C(Test)")
          if (%changed !== 0 || !%composer.skipping) {
            if (isTraceInProgress()) {
              traceEventStart(<>, %changed, -1, <>)
            }
            val foo = Foo(0)
            val bar = Bar(1)
            val lambda = <block>{
              %composer.startReplaceableGroup(<>)
              val tmpCache = %composer.cache(%composer.changedInstance(foo) or %composer.changed(bar)) {
                {
                  foo
                  bar
                }
              }
              %composer.endReplaceableGroup()
              tmpCache
            }
            if (isTraceInProgress()) {
              traceEventEnd()
            }
          } else {
            %composer.skipToGroupEnd()
          }
          %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
            Test(%composer, updateChangedFlags(%changed or 0b0001))
          }
        }
        """
    )

    @Test
    fun testFunctionInterfaceMemorized() = comparisonPropagation(
        """
            fun interface TestFunInterface {
                fun compute(value: Int)
            }
            fun use(@Suppress("UNUSED_PARAMETER") v: Int) {}
        """.trimIndent(),
        """
            @Composable fun TestMemoizedFun(compute: TestFunInterface) {}
            @Composable fun Test() {
                val capture = 0
                TestMemoizedFun {
                    // no captures
                    use(it)
                }
                TestMemoizedFun {
                    // stable captures
                    use(capture)
                }
            }
        """.trimIndent(),
        """
            @Composable
            fun TestMemoizedFun(compute: TestFunInterface, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(TestMemoizedFun)")
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
                TestMemoizedFun(compute, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val capture = 0
                TestMemoizedFun(TestFunInterface { it: Int ->
                  use(it)
                }, %composer, 0b0110)
                TestMemoizedFun(<block>{
                  %composer.startReplaceableGroup(<>)
                  val tmpCache = %composer.cache(%composer.changed(capture)) {
                    TestFunInterface { it: Int ->
                      use(capture)
                    }
                  }
                  %composer.endReplaceableGroup()
                  tmpCache
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent()
    )

    @Test
    fun testVarArgs() = comparisonPropagation(
        "",
        """
            @Composable fun Varargs(vararg ints: Int) {
            }
            @Composable fun Test() {
                Varargs(1, 2, 3)
            }
        """.trimIndent(),
        """
            @Composable
            fun Varargs(ints: IntArray, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Varargs)")
              val %dirty = %changed
              %composer.startMovableGroup(<>, ints.size)
              val <iterator> = ints.iterator()
              while (<iterator>.hasNext()) {
                val value = <iterator>.next()
                %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0
              }
              %composer.endMovableGroup()
              if (%dirty and 0b1110 === 0) {
                %dirty = %dirty or 0b0010
              }
              if (%dirty and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Varargs(*ints, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                Varargs(1, 2, 3, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """.trimIndent()
    )

    @Test
    fun testRuntimeStableVarArgs() = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            class Holder<T> {
                @Composable
                fun Test(vararg x: T) {
                    A(x as Int)
                }
            }
        """,
        """
        @StabilityInferred(parameters = 0)
        class Holder<T>  {
          @Composable
          fun Test(x: Array<out T>, %composer: Composer?, %changed: Int) {
            %composer = %composer.startRestartGroup(<>)
            sourceInformation(%composer, "C(Test)")
            val %dirty = %changed
            %composer.startMovableGroup(<>, x.size)
            val <iterator> = x.iterator()
            while (<iterator>.hasNext()) {
              val value = <iterator>.next()
              %dirty = %dirty or if (if (%changed and 0b1000 === 0) {
                %composer.changed(value)
              } else {
                %composer.changedInstance(value)
              }
              ) 0b0100 else 0
            }
            %composer.endMovableGroup()
            if (%dirty and 0b1110 === 0) {
              %dirty = %dirty or 0b0010
            }
            if (%dirty and 0b0011 !== 0b0010 || !%composer.skipping) {
              if (isTraceInProgress()) {
                traceEventStart(<>, %dirty, -1, <>)
              }
              A(x, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
            } else {
              %composer.skipToGroupEnd()
            }
            val tmp0_rcvr = <this>
            %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
              tmp0_rcvr.Test(*x, %composer, updateChangedFlags(%changed or 0b0001))
            }
          }
          static val %stable: Int = 0
        }
        """.trimIndent()
    )
}
