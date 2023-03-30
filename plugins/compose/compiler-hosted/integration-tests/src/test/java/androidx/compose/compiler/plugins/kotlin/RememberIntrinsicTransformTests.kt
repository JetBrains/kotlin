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
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.Test

class RememberIntrinsicTransformTests : AbstractIrTransformTest() {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, true)
        put(ComposeConfiguration.INTRINSIC_REMEMBER_OPTIMIZATION_ENABLED_KEY, true)
    }

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
    // The intent of this test is incorrect. If a slot is conditional it requires a group
    // as slots can only be inserted and removed as part of a group. If we made the remember in the
    // `if` statement intrinsic when taking the "else" part of the branch will leave the cached
    // state as part of the slot table (preventing `onForgotten` from being called) as its group
    // was not removed (not calling `onForgotten`). Keeping for now to ensure we handle this case
    // correctly if intrinsic remember is allowed in more places.
    //
    // If the remember in the `if` statement was converted to be an intrinsic remember the else
    // block would need to explicitly overwrite the slots used in the `then` part with
    // [Composer.Empty]. Note that this also means that the else block takens more slot space as
    // the slots are still allocated but unused.
    //
    // Without intrinsic remember, the slots are deleted from the slot table when
    // the "remember" group is removed but at the cost of a group to track the slot.
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val a = %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "<rememb...>")
              val tmp0_group = if (x) {
                remember({
                  1
                }, %composer, 0)
              } else {
                2
              }
              %composer.endReplaceableGroup()
              tmp0_group
              val b = remember({
                2
              }, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val deferred = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(key) || %changed and 0b0110 === 0b0100 or %changed and 0b01110000 xor 0b00110000 > 32 && %composer.changed(pendingResource) || %changed and 0b00110000 === 0b00100000 or %changed and 0b001110000000 xor 0b000110000000 > 256 && %composer.changed(failedResource) || %changed and 0b000110000000 === 0b000100000000) {
                123
              }
              val tmp0 = deferred > 10
              if (isTraceInProgress()) {
                traceEventEnd()
              }
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                %composer.cache(%dirty and 0b1110 === 0b0100) {
                  1
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test1(x, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
            @Composable
            fun test2(x: KnownUnstable, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(test2):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.cache(%composer.changed(x)) {
                1
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test2(x, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                %composer.cache(%dirty and 0b1110 === 0b0100 || %dirty and 0b1000 !== 0 && %composer.changed(x)) {
                  1
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                test3(x, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(x) || %changed and 0b0110 === 0b0100) {
                1
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @Composable
            @NonRestartableComposable
            fun test2(x: KnownUnstable, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(test2):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.cache(%composer.changed(x)) {
                1
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
            }
            @Composable
            @NonRestartableComposable
            fun test3(x: Uncertain, %composer: Composer?, %changed: Int) {
              %composer.startReplaceableGroup(<>)
              sourceInformation(%composer, "C(test3):Test.kt")
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(x) || %changed and 0b0110 === 0b0100) {
                1
              }
              if (isTraceInProgress()) {
                traceEventEnd()
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0 = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(a) || %changed and 0b0110 === 0b0100 or %changed and 0b01110000 xor 0b00110000 > 32 && %composer.changed(b) || %changed and 0b00110000 === 0b00100000) {
                Foo(a, b)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = %composer.cache(false) {
                  Foo()
                }
                val bar = %composer.cache(false) {
                  Foo()
                }
                A(%composer, 0)
                val bam = remember({
                  Foo()
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val a = someInt()
                val b = someInt()
                val foo = %composer.cache(%composer.changed(a) or %composer.changed(b)) {
                  Foo(a, b)
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = remember(CInt(%composer, 0), {
                  Foo()
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
              sourceInformation(%composer, "C(Test)<curren...>,<rememb...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val bar = compositionLocalBar.<get-current>(%composer, 0b0110)
                val foo = remember(bar, {
                  Foo()
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
              sourceInformation(%composer, "C(Test)<curren...>,<rememb...>:Test.kt")
              if (%changed !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = remember(compositionLocalBar.<get-current>(%composer, 0b0110), {
                  Foo()
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                A(%composer, 0)
                val foo = remember({
                  Foo()
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
              sourceInformation(%composer, "C(Test)<A()>,<rememb...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                A(%composer, 0)
                if (condition) {
                  val foo = remember({
                    Foo()
                  }, %composer, 0)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
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
              sourceInformation(%composer, "C(Test)<A()>,<rememb...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                if (condition) {
                  A(%composer, 0)
                  val foo = remember({
                    Foo()
                  }, %composer, 0)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val item = tmp0_iterator.next()
                val foo = remember({
                  Foo()
                }, %composer, 0)
                print(foo)
                print(item)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val tmp0_iterator = items.iterator()
              while (tmp0_iterator.hasNext()) {
                val item = tmp0_iterator.next()
                val foo = remember({
                  Foo()
                }, %composer, 0)
                A(%composer, 0)
                print(foo)
                print(item)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val foo = %composer.cache(false) {
                Foo()
              }
              used(items)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (%dirty and 0b0001011011011011 !== 0b010010010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100 or %dirty and 0b01110000 === 0b00100000 or %dirty and 0b001110000000 === 0b000100000000 or %dirty and 0b0001110000000000 === 0b100000000000) {
                  Foo()
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, d, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val foo = remember(*items, {
                Foo()
              }, %composer, 0)
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(items, %composer, updateChangedFlags(%changed or 0b0001))
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
                %dirty = %dirty or if (%composer.changed(<unsafe-coerce>(inlineInt))) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val a = InlineInt(123)
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100) {
                  Foo()
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(inlineInt, %composer, updateChangedFlags(%changed or 0b0001))
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
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val a = someInt()
                val b = someInt()
                val foo = %composer.cache(%composer.changed(a) or %composer.changed(b)) {
                  Foo(a, b)
                }
                val c = someInt()
                val d = someInt()
                val bar = %composer.cache(%composer.changed(c) or %composer.changed(d)) {
                  Foo(c, d)
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val b = someInt()
                val foo = %composer.cache(%dirty and 0b1110 === 0b0100 or %composer.changed(b)) {
                  Foo(a, b)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001))
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
              if (isTraceInProgress()) {
                traceEventStart(<>, %changed, -1, <>)
              }
              val b = someInt()
              val tmp0 = %composer.cache(%changed and 0b1110 xor 0b0110 > 4 && %composer.changed(a) || %changed and 0b0110 === 0b0100 or %composer.changed(b)) {
                Foo(a, b)
              }
              if (isTraceInProgress()) {
                traceEventEnd()
              }
              %composer.endReplaceableGroup()
              return tmp0
            }
        """
    )

    @Test
    fun testRememberMemoizedLambda(): Unit = comparisonPropagation(
        "",
        """
            @Composable
            fun Test(a: Int) {
                used { a }
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(%composer.cache(%dirty and 0b1110 === 0b0100) {
                  {
                    a
                  }
                }
                )
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testRememberFunctionReference(): Unit = comparisonPropagation(
        """
            fun effect(): Int = 0
        """,
        """
            @Composable
            fun Test(a: Int) {
                used(remember(a, ::effect))
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(%composer.cache(%dirty and 0b1110 === 0b0100, effect))
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testRememberAdaptedFunctionReference(): Unit = comparisonPropagation(
        """
            fun effect(a: Int = 0): Int = a
        """,
        """
            @Composable
            fun Test(a: Int) {
                used(remember(a, ::effect))
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(%composer.cache(%dirty and 0b1110 === 0b0100, {
                  effect()
                }
                ))
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )
    @Test
    fun testRememberPropertyReference(): Unit = comparisonPropagation(
        """
            class A(val value: Int)
        """.trimIndent(),
        """
            @Composable
            fun Test(a: A) {
                used(remember(a, a::value))
            }
        """,
        """
            @Composable
            fun Test(a: A, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                used(%composer.cache(%dirty and 0b1110 === 0b0100, a::value))
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test
    fun testOptimizationFailsIfDefaultsGroupIsUsed(): Unit = comparisonPropagation(
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
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = someInt()
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
                  traceEventStart(<>, %changed, -1, <>)
                }
                val foo = remember({
                  Foo()
                }, %composer, 0)
                used(foo)
                used(a)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfDefaultParameters_Simple() = comparisonPropagation(
        """""",
        """
            @Composable
            fun Test(a: Int = remember { 0 }) {
                used(a)
            }
        """,
        """
            @Composable
            fun Test(a: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test):Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a = %composer.cache(false) {
                    0
                  }
                }
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                used(a)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfDefaultParameters_AfterComposable() = comparisonPropagation(
        """
            @Composable
            fun SomeComposable() = 0
        """,
        """
            @Composable
            fun Test(a: Int = remember { 0 }, b: Int = SomeComposable(), c: Int = remember { 0 }) {
                used(a)
                used(b)
                used(c)
            }
        """,
        """
            @Composable
            fun Test(a: Int, b: Int, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<SomeCo...>,<rememb...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%default and 0b0010 === 0 && %composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = %composer.cache(false) {
                      0
                    }
                  }
                  if (%default and 0b0010 !== 0) {
                    b = SomeComposable(%composer, 0)
                    %dirty = %dirty and 0b01110000.inv()
                  }
                  if (%default and 0b0100 !== 0) {
                    c = remember({
                      0
                    }, %composer, 0)
                  }
                } else {
                  %composer.skipToGroupEnd()
                  if (%default and 0b0010 !== 0) {
                    %dirty = %dirty and 0b01110000.inv()
                  }
                }
                %composer.endDefaults()
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                used(a)
                used(b)
                used(c)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testIntrinsicRememberOfLambdaInIfBlock() = comparisonPropagation(
        // Simulation of Scrim in BackdropScaffold
        """
            class Modifier
            fun Modifier.pointerInput(key1: Any?, block: () -> Unit) = this
            fun detectTapGestures(block: () -> Unit) {}
            @Composable fun someComposableValue(): Int = 1
        """,
        """
        @Composable
        fun Test(a: Boolean, visible: Boolean, onDismiss: () -> Unit) {
            if (a) {
                val a = someComposableValue()
                used(a)
                val m = Modifier()
                val dismissModifier = if (visible) {
                    m.pointerInput(Unit) { detectTapGestures { onDismiss() } }
                } else {
                    m
                }
                used(dismissModifier)
            }
        }
        """,
        """
            @Composable
            fun Test(a: Boolean, visible: Boolean, onDismiss: Function0<Unit>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)P(!1,2)<someCo...>,<{>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(visible)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changedInstance(onDismiss)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                if (a) {
                  val a = someComposableValue(%composer, 0)
                  used(a)
                  val m = Modifier()
                  val dismissModifier =
                  val tmp0_group = if (visible) {
                    m.pointerInput(Unit, remember(onDismiss, {
                      {
                        detectTapGestures {
                          onDismiss()
                        }
                      }
                    }, %composer, 0b1110 and %dirty shr 0b0110))
                  } else {
                    m
                  }
                  tmp0_group
                  used(dismissModifier)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, visible, onDismiss, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }        """
    )

    @Test
    fun testRememberAfterStaticDefaultParameters() = comparisonPropagation(
        unchecked = """
            import androidx.compose.runtime.*

            enum class Foo {
                A,
                B,
                C,
            }

            @Stable
            fun swizzle(a: Int, b: Int) = a + b

            @Composable
            fun used(a: Any) { }
            """,
        checked = """
            @Composable
            fun Test(a: Int = 1, b: Foo = Foo.B, c: Int = swizzle(1, 2) ) {
                val s = remember(a, b, c) { Any() }
                used(s)
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(a: Int, b: Foo?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<used(s...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%default and 0b0100 !== 0) {
                %dirty = %dirty or 0b000110000000
              } else if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                if (%default and 0b0001 !== 0) {
                  a = 1
                }
                if (%default and 0b0010 !== 0) {
                  b = Foo.B
                }
                if (%default and 0b0100 !== 0) {
                  c = swizzle(1, 2)
                }
                if (isTraceInProgress()) {
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val s = %composer.cache(%dirty and 0b1110 === 0b0100 or %dirty and 0b01110000 === 0b00100000 or %dirty and 0b001110000000 === 0b000100000000) {
                  Any()
                }
                used(s, %composer, 0b1000)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testRememberAfterNonStaticDefaultParameters() = comparisonPropagation(
        unchecked = """
            import androidx.compose.runtime.*

            enum class Foo {
                A,
                B,
                C,
            }

            fun swizzle(a: Int, b: Int) = a + b

            @Composable
            fun used(a: Any) { }
            """,
        checked = """
            @Composable
            fun Test(a: Int = 1, b: Foo = Foo.B, c: Int = swizzle(1, 2) ) {
                val s = remember(a, b, c) { Any() }
                used(s)
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(a: Int, b: Foo?, c: Int, %composer: Composer?, %changed: Int, %default: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>,<used(s...>:Test.kt")
              val %dirty = %changed
              if (%default and 0b0001 !== 0) {
                %dirty = %dirty or 0b0110
              } else if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(a)) 0b0100 else 0b0010
              }
              if (%default and 0b0010 !== 0) {
                %dirty = %dirty or 0b00110000
              } else if (%changed and 0b01110000 === 0) {
                %dirty = %dirty or if (%composer.changed(b)) 0b00100000 else 0b00010000
              }
              if (%changed and 0b001110000000 === 0) {
                %dirty = %dirty or if (%default and 0b0100 === 0 && %composer.changed(c)) 0b000100000000 else 0b10000000
              }
              if (%dirty and 0b001011011011 !== 0b10010010 || !%composer.skipping) {
                %composer.startDefaults()
                if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                  if (%default and 0b0001 !== 0) {
                    a = 1
                  }
                  if (%default and 0b0010 !== 0) {
                    b = Foo.B
                  }
                  if (%default and 0b0100 !== 0) {
                    c = swizzle(1, 2)
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
                  traceEventStart(<>, %dirty, -1, <>)
                }
                val s = remember(a, b, c, {
                  Any()
                }, %composer, 0b1110 and %dirty or 0b01110000 and %dirty or 0b001110000000 and %dirty)
                used(s, %composer, 0b1000)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(a, b, c, %composer, updateChangedFlags(%changed or 0b0001), %default)
              }
            }
        """
    )

    @Test
    fun testForEarlyExit() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(condition: Boolean) {
                val value = remember { mutableStateOf(false) }
                if (!value.value && !condition) return
                val value2 = remember { mutableStateOf(false) }
                Text("Text ${'$'}{value.value}, ${'$'}{value2.value}")
            }
        """,
        expectedTransformed = """
            @Composable
            fun Test(condition: Boolean, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>,<Text("...>:Test.kt")
              val %dirty = %changed
              if (%changed and 0b1110 === 0) {
                %dirty = %dirty or if (%composer.changed(condition)) 0b0100 else 0b0010
              }
              if (%dirty and 0b1011 !== 0b0010 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val value = %composer.cache(false) {
                  mutableStateOf(
                    value = false
                  )
                }
                if (!value.value && !condition) {
                  if (isTraceInProgress()) {
                    traceEventEnd()
                  }
                  %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                    Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
                  }
                  return
                }
                val value2 = remember({
                  mutableStateOf(
                    value = false
                  )
                }, %composer, 0)
                Text("Text %{value.value}, %{value2.value}", %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(condition, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }
        """
    )

    @Test
    fun testVarargsIntrinsicRemember() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test(vararg strings: String) {
                val show = remember { mutableStateOf(false) }
                if (show.value) {
                    Text("Showing")
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) { }
        """,
        expectedTransformed = """
            @Composable
            fun Test(strings: Array<out String>, %composer: Composer?, %changed: Int) {
              %composer = %composer.startRestartGroup(<>)
              sourceInformation(%composer, "C(Test)<rememb...>,<Text("...>:Test.kt")
              val %dirty = %changed
              %composer.startMovableGroup(<>, strings.size)
              val tmp0_iterator = strings.iterator()
              while (tmp0_iterator.hasNext()) {
                val value = tmp0_iterator.next()
                %dirty = %dirty or if (%composer.changed(value)) 0b0100 else 0
              }
              %composer.endMovableGroup()
              if (%dirty and 0b1110 === 0) {
                %dirty = %dirty or 0b0010
              }
              if (%dirty and 0b0001 !== 0 || !%composer.skipping) {
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val show = remember({
                  mutableStateOf(
                    value = false
                  )
                }, %composer, 0)
                if (show.value) {
                  Text("Showing", %composer, 0b0110)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              } else {
                %composer.skipToGroupEnd()
              }
              %composer.endRestartGroup()?.updateScope { %composer: Composer?, %force: Int ->
                Test(*strings, %composer, updateChangedFlags(%changed or 0b0001))
              }
            }
        """
    )

    @Test // regression test for b/267586102
    fun testRememberInALoop() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            val content: @Composable (a: SomeUnstableClass) -> Unit = {
                for (index in 0 until count) {
                    val i = remember { index }
                }
                val a = remember { 1 }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            val count = 0
            class SomeUnstableClass(val a: Any = "abc")
        """,
        expectedTransformed = """
            val content: Function3<@[ParameterName(name = 'a')] SomeUnstableClass, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function3<SomeUnstableClass, Composer, Int, Unit> = composableLambdaInstance(<>, false) { it: SomeUnstableClass, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C<rememb...>:Test.kt")
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                %composer.startReplaceableGroup(<>)
                sourceInformation(%composer, "*<rememb...>")
                val tmp0_iterator = 0 until count.iterator()
                while (tmp0_iterator.hasNext()) {
                  val index = tmp0_iterator.next()
                  val i = remember({
                    index
                  }, %composer, 0)
                }
                %composer.endReplaceableGroup()
                val a = remember({
                  1
                }, %composer, 0)
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              }
            }

        """
    )

    @Test // Regression test for b/267586102 to ensure the fix doesn't insert unnecessary groups
    fun testRememberInALoop_NoTrailingRemember() = verifyComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            val content: @Composable (a: SomeUnstableClass) -> Unit = {
                for (index in 0 until count) {
                    val i = remember { index }
                }
            }
        """,
        extra = """
                import androidx.compose.runtime.*

                val count = 0
                class SomeUnstableClass(val a: Any = "abc")
            """,
        expectedTransformed = """
            val content: Function3<@[ParameterName(name = 'a')] SomeUnstableClass, Composer, Int, Unit> = ComposableSingletons%TestKt.lambda-1
            internal object ComposableSingletons%TestKt {
              val lambda-1: Function3<SomeUnstableClass, Composer, Int, Unit> = composableLambdaInstance(<>, false) { it: SomeUnstableClass, %composer: Composer?, %changed: Int ->
                sourceInformation(%composer, "C*<rememb...>:Test.kt")
                if (isTraceInProgress()) {
                  traceEventStart(<>, %changed, -1, <>)
                }
                val tmp0_iterator = 0 until count.iterator()
                while (tmp0_iterator.hasNext()) {
                  val index = tmp0_iterator.next()
                  val i = remember({
                    index
                  }, %composer, 0)
                }
                if (isTraceInProgress()) {
                  traceEventEnd()
                }
              }
            }
        """
    )
}
