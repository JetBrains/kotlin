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
import org.junit.Ignore
import org.junit.Test

class RememberIntrinsicTransformTests : ComposeIrTransformTest() {
    private fun comparisonPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        expectedTransformed: String,
        dumpTree: Boolean = false
    ) = verifyComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember

            $checked
        """.trimIndent(),
        expectedTransformed,
        """
            import androidx.compose.runtime.Composable

            $unchecked
            fun used(x: Any?) {}
        """.trimIndent(),
        dumpTree = dumpTree
    )

    @Test
    fun testElidedRememberInsideIfDeoptsRememberAfterIf(): Unit = comparisonPropagation(
        "",
        """
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            @NonRestartableComposable
            fun app(x: Boolean) {
                val a = if (x) { remember { 1 } } else { 2 }
                val b = remember { 2 }
            }
        """,
        """
            @Composable
            @NonRestartableComposable
            fun app(x: Boolean, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(app)<rememb...>:Test.kt")
              val a = if (x) {
                %composer.startReplaceableGroup(<>)
                val tmp0_group = %composer.cache(false) {
                  val tmp0_return = 1
                  tmp0_return
                }
                %composer.endReplaceableGroup()
                tmp0_group
              } else {
                %composer.startReplaceableGroup(<>)
                %composer.endReplaceableGroup()
                2
              }
              val b = remember({
                val tmp0_return = 2
                tmp0_return
              }, %composer, 0)
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testMultipleParamInputs(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun <T> loadResourceInternal(
                key: String,
                pendingResource: T? = null,
                failedResource: T? = null
            ): Boolean {
                val deferred = remember(key, pendingResource, failedResource) {
                    123
                }
                return deferred > 10
            }
        """,
        """
            @Composable
            fun <T> loadResourceInternal(key: String, pendingResource: T?, failedResource: T?, %composer: Composer?, %changed: Int, %default: Int): Boolean {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(loadResourceInternal)P(1,2):Test.kt")
              if (%default and 0b0010 !== 0) {
                pendingResource = null
              }
              if (%default and 0b0100 !== 0) {
                failedResource = null
              }
              val deferred = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(key) || %changed and 0b0110 === 0b0100 or %changed and 0b01110000 xor 0b00110000 > 32 && %composer.changed(pendingResource) || %changed and 0b00110000 === 0b00100000 or %changed and 0b001110000000 xor 0b000110000000 > 256 && %composer.changed(failedResource) || %changed and 0b000110000000 === 0b000100000000) {
                val tmp0_return = 123
                tmp0_return
              }
              val tmp0 = deferred > 10
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testRestartableParameterInputsStableUnstableUncertain(): Unit = comparisonPropagation(
        """
            class KnownStable
            class KnownUnstable(var x: Int)
            interface Uncertain
        """,
        """
            @Composable
            fun test1(x: KnownStable) {
                remember(x) { 1 }
            }
            @Composable
            fun test2(x: KnownUnstable) {
                remember(x) { 1 }
            }
            @Composable
            fun test3(x: Uncertain) {
                remember(x) { 1 }
            }
        """,
        """
            @Composable
            fun test1(x: KnownStable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test1):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                %composer.cache(%dirty and 0b1110 === 0b0100) {
                  val tmp0_return = 1
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test1(x, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun test2(x: KnownUnstable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test2):Test.kt")
              %composer.cache(%composer.changed(x)) {
                val tmp0_return = 1
                tmp0_return
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test2(x, %composer, %changed or 0b0001)
              }
            }
            @Composable
            fun test3(x: Uncertain, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test3):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(x)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                %composer.cache(%dirty and 0b1110 === 0b0100 || %dirty and 0b1000 !== 0 && %composer.changed(x)) {
                  val tmp0_return = 1
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test3(x, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testNonRestartableParameterInputsStableUnstableUncertain(): Unit = comparisonPropagation(
        """
            class KnownStable
            class KnownUnstable(var x: Int)
            interface Uncertain
        """,
        """
            import androidx.compose.runtime.NonRestartableComposable

            @Composable
            @NonRestartableComposable
            fun test1(x: KnownStable) {
                remember(x) { 1 }
            }
            @Composable
            @NonRestartableComposable
            fun test2(x: KnownUnstable) {
                remember(x) { 1 }
            }
            @Composable
            @NonRestartableComposable
            fun test3(x: Uncertain) {
                remember(x) { 1 }
            }
        """,
        """
            @Composable
            @NonRestartableComposable
            fun test1(x: KnownStable, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(test1):Test.kt")
              %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(x) || %changed and 0b0110 === 0b0100) {
                val tmp0_return = 1
                tmp0_return
              }
              %composer.endReplaceableGroup()
            }
            @Composable
            @NonRestartableComposable
            fun test2(x: KnownUnstable, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(test2):Test.kt")
              %composer.cache(%composer.changed(x)) {
                val tmp0_return = 1
                tmp0_return
              }
              %composer.endReplaceableGroup()
            }
            @Composable
            @NonRestartableComposable
            fun test3(x: Uncertain, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(test3):Test.kt")
              %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(x) || %changed and 0b0110 === 0b0100) {
                val tmp0_return = 1
                tmp0_return
              }
              %composer.endReplaceableGroup()
            }
        """
    )

    @Test
    fun testPassedArgs(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
        """,
        """
            @Composable
            fun rememberFoo(a: Int, b: Int) = remember(a, b) { Foo(a, b) }
        """,
        """
            @Composable
            fun rememberFoo(a: Int, b: Int, %composer: Composer?, %changed: Int): Foo {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(rememberFoo):Test.kt")
              val tmp0 = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(a) || %changed and 0b0110 === 0b0100 or %changed and 0b01110000 xor 0b00110000 > 32 && %composer.changed(b) || %changed and 0b00110000 === 0b00100000) {
                val tmp0_return = Foo(a, b)
                tmp0_return
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testNoArgs(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A(){}
        """,
        """
            @Composable
            fun Test() {
                val foo = remember { Foo() }
                val bar = remember { Foo() }
                A()
                val bam = remember { Foo() }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<rememb...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val foo = %composer.cache(false) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
                val bar = %composer.cache(false) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
                A(%composer, 0)
                val bam = remember({
                  val tmp0_return = Foo()
                  tmp0_return
                }, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testNonArgs(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test() {
                val a = someInt()
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val a = someInt()
                val b = someInt()
                val foo = %composer.cache(%composer.changed(a) or %composer.changed(b)) {
                  val tmp0_return = Foo(a, b)
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testComposableCallInArgument(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun CInt(): Int { return 123 }
        """,
        """
            @Composable
            fun Test() {
                val foo = remember(CInt()) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<CInt()...>,<rememb...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val foo = remember(CInt(%composer, 0), {
                  val tmp0_return = Foo()
                  tmp0_return
                }, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testCompositionLocalCallBeforeRemember(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            class Foo
            class Bar
            val compositionLocalBar = compositionLocalOf<Bar> { Bar() }
        """,
        """
            @Composable
            fun Test() {
                val bar = compositionLocalBar.current
                val foo = remember(bar) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<curren...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val bar = compositionLocalBar.current
                val foo = %composer.cache(%composer.changed(bar)) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testCompositionLocalCallAsInput(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            class Foo
            class Bar
            val compositionLocalBar = compositionLocalOf<Bar> { Bar() }
        """,
        """
            @Composable
            fun Test() { 
                val foo = remember(compositionLocalBar.current) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<curren...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val foo = %composer.cache(%composer.changed(compositionLocalBar.current)) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testComposableCallBeforeRemember(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() { }
        """,
        """
            @Composable
            fun Test() {
                A()
                val foo = remember { Foo() }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>,<rememb...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                A(%composer, 0)
                val foo = remember({
                  val tmp0_return = Foo()
                  tmp0_return
                }, %composer, 0)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberInsideOfIf(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(condition: Boolean) {
                A()
                if (condition) {
                    val foo = remember { Foo() }
                }
            }
        """,
        """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<A()>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                A(%composer, 0)
                if (condition) {
                  %composer.startReplaceableGroup(<>)
                  val foo = %composer.cache(false) {
                    val tmp0_return = Foo()
                    tmp0_return
                  }
                  %composer.endReplaceableGroup()
                } else {
                  %composer.startReplaceableGroup(<>)
                  %composer.endReplaceableGroup()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberInsideOfIfWithComposableCallBefore(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(condition: Boolean) {
                if (condition) {
                    A()
                    val foo = remember { Foo() }
                }
            }
        """,
        """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (condition) {
                  %composer.startReplaceableGroup(<>)
                  sourceInformation(%composer, "<A()>,<rememb...>")
                  A(%composer, 0)
                  val foo = remember({
                    val tmp0_return = Foo()
                    tmp0_return
                  }, %composer, 0)
                  %composer.endReplaceableGroup()
                } else {
                  %composer.startReplaceableGroup(<>)
                  %composer.endReplaceableGroup()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberInsideOfWhileWithOnlyRemembers(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                for (item in items) {
                    val foo = remember { Foo() }
                    print(foo)
                    print(item)
                }
            }
        """,
        """
            @Composable
            fun Test(items: List<Int>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<rememb...>:Test.kt")
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val item = tmp0_iterator.next()
                val foo = remember({
                  val tmp0_return = Foo()
                  tmp0_return
                }, %composer, 0)
                print(foo)
                print(item)
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberInsideOfWhileWithCallsAfter(): Unit = comparisonPropagation(
        """
            class Foo
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                for (item in items) {
                    val foo = remember { Foo() }
                    A()
                    print(foo)
                    print(item)
                }
            }
        """,
        """
            @Composable
            fun Test(items: List<Int>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)*<rememb...>,<A()>:Test.kt")
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val item = tmp0_iterator.next()
                val foo = remember({
                  val tmp0_return = Foo()
                  tmp0_return
                }, %composer, 0)
                A(%composer, 0)
                print(foo)
                print(item)
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testZeroArgRemember(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            @Composable
            fun Test(items: List<Int>) {
                val foo = remember { Foo() }
                used(items)
            }
        """,
        """
            @Composable
            fun Test(items: List<Int>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val foo = %composer.cache(false) {
                val tmp0_return = Foo()
                tmp0_return
              }
              used(items)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberWithNArgs(): Unit = comparisonPropagation(
        """
            class Foo
            class Bar
        """,
        """
            @Composable
            fun Test(a: Int, b: Int, c: Bar, d: Boolean) {
                val foo = remember(a, b, c, d) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(a: Int, b: Int, c: Bar, d: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%changed and 0b0001110000000000 === 0) {
                %dirty = %dirty or if (%composer.changed(d)) 0b100000000000 else 0b010000000000
              }
              if (%dirty and 0b0001011011011011 xor 0b010010010010 !== 0 || !%composer.skipping) {
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100 or %dirty and 0b01110000 === 0b00100000 or %dirty and 0b001110000000 === 0b000100000000 or %dirty and 0b0001110000000000 === 0b100000000000) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, d, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testVarargWithSpread(): Unit = comparisonPropagation(
        """
            class Foo
            class Bar
        """,
        """
            @Composable
            fun Test(items: Array<Bar>) {
                val foo = remember(*items) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(items: Array<Bar>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>:Test.kt")
              val foo = remember(*items, {
                val tmp0_return = Foo()
                tmp0_return
              }, %composer, 0)
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testRememberWithInlineClassInput(): Unit = comparisonPropagation(
        """
            class Foo
            inline class InlineInt(val value: Int)
        """,
        """
            @Composable
            fun Test(inlineInt: InlineInt) {
                val a = InlineInt(123)
                val foo = remember(inlineInt, a) { Foo() }
            }
        """,
        """
            @Composable
            fun Test(inlineInt: InlineInt, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(0:InlineInt):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(inlineInt.value)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                val a = InlineInt(123)
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100 or false) {
                  val tmp0_return = Foo()
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(inlineInt, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testMultipleRememberCallsInARow(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test() {
                val a = someInt()
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
                val c = someInt()
                val d = someInt()
                val bar = remember(c, d) { Foo(c, d) }
            }
        """,
        """
            @Composable
            fun Test(%composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                val a = someInt()
                val b = someInt()
                val foo = %composer.cache(%composer.changed(a) or %composer.changed(b)) {
                  val tmp0_return = Foo(a, b)
                  tmp0_return
                }
                val c = someInt()
                val d = someInt()
                val bar = %composer.cache(%composer.changed(c) or %composer.changed(d)) {
                  val tmp0_return = Foo(c, d)
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(%composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testParamAndNonParamInputsInRestartableFunction(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int) {
                val b = someInt()
                val foo = remember(a, b) { Foo(a, b) }
            }
        """,
        """
            @Composable
            fun Test(a: Int, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                val b = someInt()
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100 or %composer.changed(b)) {
                  val tmp0_return = Foo(a, b)
                  tmp0_return
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, %changed or 0b0001)
              }
            }
        """
    )

    @Test
    fun testParamAndNonParamInputsInDirectFunction(): Unit = comparisonPropagation(
        """
            class Foo(val a: Int, val b: Int)
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int): Foo {
                val b = someInt()
                return remember(a, b) { Foo(a, b) }
            }
        """,
        """
            @Composable
            fun Test(a: Int, %composer: Composer?, %changed: Int): Foo {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val b = someInt()
              val tmp0 = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(a) || %changed and 0b0110 === 0b0100 or %composer.changed(b)) {
                val tmp0_return = Foo(a, b)
                tmp0_return
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Ignore("This test must pass before intrinsic remember can be turned on")
    fun xtestOptimizationFailsIfDefaultsGroupIsUsed(): Unit = comparisonPropagation(
        """
            class Foo
            fun someInt(): Int = 123
        """,
        """
            @Composable
            fun Test(a: Int = someInt()) {
                val foo = remember { Foo() }
                used(foo)
                used(a)
            }
        """,
        """
            @Composable
            fun Test(a: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%default and 0b0001 === 0 && %composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 xor 0b0010 !== 0 || !%composer.skipping) {
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  %composer.startDefaults()
                  if (%default and 0b0001 !== 0) {
                    a = someInt()
                    %dirty = %dirty and 0b1110.inv()
                  }
                  %composer.endDefaults()
                } else {
                  %composer.skipCurrentGroup()
                  if (%default and 0b0001 !== 0) {
                    %dirty = %dirty and 0b1110.inv()
                  }
                }
                val foo = remember({
                  Foo()
                }, %composer, 0)
                used(foo)
                used(a)
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, %changed or 0b0001, %default)
              }
            }
        """
    )
}