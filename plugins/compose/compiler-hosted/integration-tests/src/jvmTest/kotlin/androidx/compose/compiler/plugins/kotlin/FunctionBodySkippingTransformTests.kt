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
import org.junit.Ignore
import org.junit.Test

abstract class FunctionBodySkippingTransformTestsBase(
    useFir: Boolean
) : AbstractIrTransformTest(useFir) {
    protected fun comparisonPropagation(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        dumpTree: Boolean = false
    ) = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.ReadOnlyComposable

            $checked
        """.trimIndent(),
        """
            import androidx.compose.runtime.Composable

            $unchecked
            fun used(x: Any?) {}
        """.trimIndent(),
        dumpTree = dumpTree
    )
}

class FunctionBodySkippingTransformTests(
    useFir: Boolean
) : FunctionBodySkippingTransformTestsBase(useFir) {
    @Test
    fun testIfInLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
            @Composable fun Wrap(content: @Composable () -> Unit) {
                content()
            }
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                used(y)
                Wrap {
                    if (x > 0) {
                        A(x)
                    } else {
                        A(x)
                    }
                }
            }
        """
    )

    @Test
    fun testBasicText(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.ui.text.style.TextOverflow
            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.TextLayoutResult

            @Composable
            fun BasicText(
                style: TextStyle = TextStyle.Default,
                onTextLayout: (TextLayoutResult) -> Unit = {},
                overflow: TextOverflow = TextOverflow.Clip,
            ) {
                used(style)
                used(onTextLayout)
                used(overflow)
            }
        """
    )

    @Test
    fun testArrangement(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.Arrangement.Vertical

            @Composable
            fun A(
                arrangement: Vertical = Arrangement.Top
            ) {
                used(arrangement)
            }
        """
    )

    @Test
    fun testComposableSingletonsAreStatic(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun Example(
                content: @Composable () -> Unit = {}
            ) {
                content()
            }
        """
    )

    @Test
    fun testFunInterfaces(): Unit = comparisonPropagation(
        """
            fun interface A {
                @Composable fun compute(value: Int): Unit
            }
        """,
        """
            fun Example(a: A) {
                used(a)
                Example { it -> a.compute(it) }
            }
        """
    )

    @Test
    fun testFunInterfaces2(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Immutable
            import androidx.compose.runtime.Stable

            @Stable
            fun Color(color: Long): Color {
                return Color((color shl 32).toULong())
            }

            @Immutable
            @kotlin.jvm.JvmInline
            value class Color(val value: ULong) {
                companion object {
                    @Stable
                    val Red = Color(0xFFFF0000)
                    @Stable
                    val Blue = Color(0xFF0000FF)
                    @Stable
                    val Transparent = Color(0x00000000)
                }
            }

            @Composable
            public fun Text(
                text: String,
                color: Color = Color.Transparent,
                softWrap: Boolean = true,
                maxLines: Int = Int.MAX_VALUE,
                minLines: Int = 1,
            ) {}

            @Composable fun condition(): Boolean = true

            fun interface ButtonColors {
                @Composable fun getColor(): Color
            }
        """,
        """
            @Composable
            fun Button(colors: ButtonColors) {
                Text("hello world", color = colors.getColor())
            }
            @Composable
            fun Test() {
                Button {
                    if (condition()) Color.Red else Color.Blue
                }
            }
        """
    )

    @Test
    fun testSimpleColumn(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable
            import androidx.compose.runtime.Immutable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }

            @Immutable
            interface Arrangement {
              @Immutable
              interface Vertical : Arrangement

              object Top : Vertical
            }

            enum class LayoutOrientation {
                Horizontal,
                Vertical
            }

            enum class SizeMode {
                Wrap,
                Expand
            }

            @Immutable
            data class Alignment(
                val verticalBias: Float,
                val horizontalBias: Float
            ) {
                @Immutable
                data class Horizontal(val bias: Float)

                companion object {
                  val Start = Alignment.Horizontal(-1f)
                }
            }
        """,
        """
            @Composable
            fun RowColumnImpl(
              orientation: LayoutOrientation,
              modifier: Modifier = Modifier,
              arrangement: Arrangement.Vertical = Arrangement.Top,
              crossAxisAlignment: Alignment.Horizontal = Alignment.Start,
              crossAxisSize: SizeMode = SizeMode.Wrap,
              content: @Composable() ()->Unit
            ) {
                used(orientation)
                used(modifier)
                used(arrangement)
                used(crossAxisAlignment)
                used(crossAxisSize)
                content()
            }

            @Composable
            fun Column(
                modifier: Modifier = Modifier,
                verticalArrangement: Arrangement.Vertical = Arrangement.Top,
                horizontalGravity: Alignment.Horizontal = Alignment.Start,
                content: @Composable() ()->Unit
            ) {
              RowColumnImpl(
                orientation = LayoutOrientation.Vertical,
                arrangement = verticalArrangement,
                crossAxisAlignment = horizontalGravity,
                crossAxisSize = SizeMode.Wrap,
                modifier = modifier,
                content = content
              )
            }
        """
    )

    @Test
    fun testSimplerBox(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier) {
               used(modifier)
            }
        """
    )

    @Test
    fun testDefaultSkipping(): Unit = comparisonPropagation(
        """
            fun newInt(): Int = 123
        """,
        """
            @Composable
            fun Example(a: Int = newInt()) {
               print(a)
            }
        """
    )

    @Test
    fun testLocalComposableFunctions(): Unit = comparisonPropagation(
        """
            @Composable fun A(a: Int) {}
        """,
        """
            @Composable
            fun Example(a: Int) {
                @Composable fun Inner() {
                    A(a)
                }
                Inner()
            }
        """
    )

    @Test
    fun testLoopWithContinueAndCallAfter(): Unit = comparisonPropagation(
        """
            @Composable fun Call() {}
            fun condition(): Boolean = true
        """,
        """
            @Composable
            @NonRestartableComposable
            fun Example() {
                Call()
                for (index in 0..1) {
                    Call()
                    if (condition())
                        continue
                    Call()
                }
            }
        """
    )

    @Test
    fun testSimpleBoxWithShape(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }

            interface Shape {
            }

            val RectangleShape = object : Shape { }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
                used(modifier)
                used(shape)
            }
        """
    )

    @Test
    fun testSimpleBox(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
        """,
        """
            @Composable
            fun SimpleBox(modifier: Modifier = Modifier, content: @Composable() () -> Unit = {}) {
                used(modifier)
                content()
            }
        """
    )

    @Test
    fun testComposableLambdaWithStableParams(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Immutable

            @Immutable class Foo
            @Composable fun A(x: Int) {}
            @Composable fun B(y: Foo) {}
        """,
        """
            val foo = @Composable { x: Int, y: Foo ->
                A(x)
                B(y)
            }
        """
    )

    @Test
    fun testComposableLambdaWithUnstableParams(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
            @Composable fun A(x: Int) {}
            @Composable fun B(y: Foo) {}
        """,
        """
            val foo = @Composable { x: Int, y: Foo ->
                A(x)
                B(y)
            }
        """
    )

    @Test
    fun testComposableLambdaWithStableParamsAndReturnValue(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable fun SomeThing(content: @Composable() () -> Unit) { content() }

            @Composable
            fun Example() {
                SomeThing {
                    val id = object {}
                }
            }
        """
    )

    @Test
    fun testPrimitiveVarargParams(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun B(vararg values: Int) {
                print(values)
            }
        """
    )

    @Test
    fun testStableVarargParams(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Immutable
            @Immutable class Foo
        """,
        """
            @Composable
            fun B(vararg values: Foo) {
                print(values)
            }
        """
    )

    @Test
    fun testUnstableVarargParams(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
        """,
        """
            @Composable
            fun B(vararg values: Foo) {
                print(values)
            }
        """
    )

    @Test
    fun testReceiverParamSkippability(): Unit = comparisonPropagation(
        """
        """,
        """
            class Foo {
             var counter: Int = 0
             @Composable fun A() {
                print("hello world")
             }
             @Composable fun B() {
                print(counter)
             }
            }
        """
    )

    @Test
    fun testComposableParameter(): Unit = comparisonPropagation(
        """
            @Composable fun makeInt(): Int = 10
        """,
        """
            @Composable
            fun Example(a: Int = 0, b: Int = makeInt(), c: Int = 0) {
                used(a)
                used(b)
                used(c)
            }
        """
    )

    @Test
    fun testComposableWithAndWithoutDefaultParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable fun Wrap(y: Int, content: @Composable (x: Int) -> Unit) {
                content(y)
            }
            @Composable
            fun Test(x: Int = 0, y: Int = 0) {
                used(y)
                Wrap(10) {
                    used(it)
                    A(x)
                }
            }
        """
    )

    @Test
    fun testComposableWithReturnValue(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0, y: Int = 0): Int {
                A(x, y)
                return x + y
            }
        """
    )

    @Test
    fun testComposableLambda(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0) {}
        """,
        """
            val test = @Composable { x: Int ->
                A(x)
            }
        """
    )

    @Test
    fun testComposableFunExprBody(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int) = A()
        """
    )

    @Test
    fun testParamReordering(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int = 0, y: Int = 0): Int { return 10 }
        """,
        """
            @Composable fun Test(x: Int, y: Int) {
                A(y = y, x = x)
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
        """
    )

    @Test
    fun testOptionalUnstableWithStableExtensionReceiver(): Unit = comparisonPropagation(
        """
            class Foo(var value: Int = 0)
            class Bar
        """,
        """
            @Composable fun Bar.CanSkip(b: Foo = Foo()) {
                print("Hello World")
            }
        """
    )

    @Test
    fun testNoParams(): Unit = comparisonPropagation(
        """
            @Composable fun A() {}
        """,
        """
            @Composable
            fun Test() {
                A()
            }
        """
    )

    @Test
    fun testSingleStableParam(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int) {
                A(x)
            }
        """
    )

    @Test
    fun testInlineClassDefaultParameter(): Unit = comparisonPropagation(
        """
            inline class Color(val value: Int) {
                companion object {
                    val Unset = Color(0)
                }
            }
        """,
        """
            @Composable
            fun A(text: String) {
                B(text)
            }

            @Composable
            fun B(text: String, color: Color = Color.Unset) {
                used(text)
                used(color)
            }
        """
    )

    @Test
    fun testStaticDetection(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            enum class Foo {
                Bar,
                Bam
            }
            const val constInt: Int = 123
            val normInt = 345
            val stableTopLevelProp: Modifier = Modifier
            @Composable fun C(x: Any?) {}
            @Stable
            interface Modifier {
              companion object : Modifier { }
            }
            inline class Dp(val value: Int)
            @Stable
            fun stableFun(x: Int): Int = x * x
            @Stable
            operator fun Dp.plus(other: Dp): Dp = Dp(this.value + other.value)
            @Stable
            val Int.dp: Dp get() = Dp(this)
            @Composable fun D(content: @Composable() () -> Unit) {}
        """,
        """
            // all of these should result in 0b0110
            @Composable fun A() {
                val x = 123
                D {}
                C({})
                C(stableFun(123))
                C(16.dp + 10.dp)
                C(Dp(16))
                C(16.dp)
                C(normInt)
                C(Int.MAX_VALUE)
                C(stableTopLevelProp)
                C(Modifier)
                C(Foo.Bar)
                C(constInt)
                C(123)
                C(123 + 345)
                C(x)
                C(x * 123)
            }
            // all of these should result in 0b0000
            @Composable fun B() {
                C(Math.random())
                C(Math.random() / 100f)
            }
        """
    )

    @Test
    fun testAnnotationChecker(): Unit = comparisonPropagation(
        """
            @Composable fun D(content: @Composable() () -> Unit) {}
        """,
        """
            @Composable fun Example() {
                D {}
            }
        """
    )

    @Test
    fun testSingleStableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
        """,
        """
            @Composable
            fun Test(x: Int = 0) {
                A(x)
            }
        """
    )

    @Test
    fun testSingleStableParamWithComposableDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Int) {}
            @Composable fun I(): Int { return 10 }
        """,
        """
            @Composable
            fun Test(x: Int = I()) {
                A(x)
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
        """
    )

    @Test
    fun testSingleUnstableParamWithDefault(): Unit = comparisonPropagation(
        """
            @Composable fun A(x: Foo) {}
            class Foo
        """,
        """
            @Composable
            fun Test(x: Foo = Foo()) {
                A(x)
            }
        """
    )

    @Test
    fun testManyNonOptionalParams(): Unit = comparisonPropagation(
        """
            @Composable fun A(a: Int, b: Boolean, c: Int, d: Foo, e: List<Int>) {}
            class Foo
        """,
        """
            @Composable
            fun Test(a: Int, b: Boolean, c: Int = 0, d: Foo = Foo(), e: List<Int> = emptyList()) {
                A(a, b, c, d, e)
            }
        """
    )

    @Test
    fun testRecursiveCall(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable
            fun X(x: Int) {
                X(x + 1)
                X(x)
            }
        """
    )

    @Test
    fun testLambdaSkipping(): Unit = comparisonPropagation(
        """
        import androidx.compose.runtime.*

        data class User(
            val id: Int,
            val name: String
        )

        interface LazyPagingItems<T> {
            val itemCount: Int
            operator fun get(index: Int): State<T?>
        }

        @Stable interface LazyListScope {
            fun items(itemCount: Int, itemContent: @Composable LazyItemScope.(Int) -> Unit)
        }

        @Stable interface LazyItemScope

        public fun <T : Any> LazyListScope.itemsIndexed(
            lazyPagingItems: LazyPagingItems<T>,
            itemContent: @Composable LazyItemScope.(Int, T?) -> Unit
        ) {
            items(lazyPagingItems.itemCount) { index ->
                val item = lazyPagingItems[index].value
                itemContent(index, item)
            }
        }
        """,
        """
            fun LazyListScope.Example(items: LazyPagingItems<User>) {
                itemsIndexed(items) { index, user ->
                    print("Hello World")
                }
            }
        """
    )

    @Test
    fun testPassedExtensionWhenExtensionIsPotentiallyUnstable(): Unit = comparisonPropagation(
        """
            interface Unstable
        """,
        """
            @Composable fun Unstable.Test() {
                doSomething(this) // does this reference %dirty without %dirty
            }

            @Composable fun doSomething(x: Unstable) {}
        """
    )

    @Test
    fun testReceiverIssue(): Unit = comparisonPropagation(
        """
            class Foo
        """,
        """
            import androidx.compose.runtime.ExplicitGroupsComposable

            @Composable
            @ExplicitGroupsComposable
            fun A(foo: Foo) {
                foo.b()
            }

            @Composable
            @ExplicitGroupsComposable
            inline fun Foo.b(label: String = "") {
                c(this, label)
            }

            @Composable
            @ExplicitGroupsComposable
            inline fun c(foo: Foo, label: String) {
                print(label)
            }
        """
    )

    @Test
    fun testDifferentParameters(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int, c: Int, d: Int) {}
            val fooGlobal = 10
        """,
        """
            @Composable
            fun A(x: Int) {
                B(
                    // direct parameter
                    x,
                    // transformation
                    x + 1,
                    // literal
                    123,
                    // expression with no parameter
                    fooGlobal
                )
            }
        """
    )

    @Test
    fun testReceiverLambdaCall(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.Stable

            interface Foo { val x: Int }
            @Stable
            interface StableFoo { val x: Int }
        """,
        """
            val unstableUnused: @Composable Foo.() -> Unit = {
            }
            val unstableUsed: @Composable Foo.() -> Unit = {
                used(x)
            }
            val stableUnused: @Composable StableFoo.() -> Unit = {
            }
            val stableUsed: @Composable StableFoo.() -> Unit = {
                used(x)
            }
        """
    )

    @Test
    fun testNestedCalls(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int = 0, b: Int = 0, c: Int = 0) {}
            @Composable fun Provide(content: @Composable (Int) -> Unit) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                Provide { y ->
                    Provide { z ->
                        B(x, y, z)
                    }
                    B(x, y)
                }
                B(x)
            }
        """
    )

    @Test
    fun testLocalFunction(): Unit = comparisonPropagation(
        """
            @Composable fun B(a: Int, b: Int) {}
        """,
        """
            @Composable
            fun A(x: Int) {
                @Composable fun foo(y: Int) {
                    B(x, y)
                }
                foo(x)
            }
        """
    )

    @Test
    fun test15Parameters(): Unit = comparisonPropagation(
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
                a14: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14
                )
                // in opposite order
                Example(
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """
    )

    @Test
    fun test16Parameters(): Unit = comparisonPropagation(
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
                a15: Int = 0
            ) {
                // in order
                Example(
                    a00,
                    a01,
                    a02,
                    a03,
                    a04,
                    a05,
                    a06,
                    a07,
                    a08,
                    a09,
                    a10,
                    a11,
                    a12,
                    a13,
                    a14,
                    a15
                )
                // in opposite order
                Example(
                    a15,
                    a14,
                    a13,
                    a12,
                    a11,
                    a10,
                    a09,
                    a08,
                    a07,
                    a06,
                    a05,
                    a04,
                    a03,
                    a02,
                    a01,
                    a00
                )
            }
        """
    )

    @Test
    fun testGrouplessProperty(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.currentComposer

            open class Foo {
                inline val current: Int
                    @Composable
                    @ReadOnlyComposable get() = currentComposer.hashCode()

                @ReadOnlyComposable
                @Composable
                fun getHashCode(): Int = currentComposer.hashCode()
            }

            @ReadOnlyComposable
            @Composable
            fun getHashCode(): Int = currentComposer.hashCode()
        """
    )

    @Test
    fun testStaticAndNonStaticDefaultValueSkipping(): Unit = comparisonPropagation(
        """
            import androidx.compose.runtime.compositionLocalOf

            val LocalColor = compositionLocalOf { 123 }
            @Composable fun A(a: Int) {}
        """,
        """
            @Composable
            fun Example(
                wontChange: Int = 123,
                mightChange: Int = LocalColor.current
            ) {
                A(wontChange)
                A(mightChange)
            }
        """
    )

    @Test
    fun testComposableLambdaInvoke(): Unit = comparisonPropagation(
        """
        """,
        """
            @Composable fun Example(content: @Composable() () -> Unit) {
                content.invoke()
            }
        """
    )

    @Test
    fun testComposableLambdasWithReturnGetGroups(): Unit = comparisonPropagation(
        """
        """,
        """
            fun A(factory: @Composable () -> Int): Unit {}
            fun B() = A { 123 }
        """
    )

    @Test
    fun testDefaultsIssue(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp

            @Composable
            fun Box2(
                modifier: Modifier = Modifier,
                paddingStart: Dp = Dp.Unspecified,
                content: @Composable () -> Unit = {}
            ) {
                used(modifier)
                used(paddingStart)
                content()
            }
        """
    )

    @Test
    fun testSiblingIfsWithoutElseHaveUniqueKeys(): Unit = comparisonPropagation(
        """
            @Composable fun A(){}
            @Composable fun B(){}
        """,
        """
            @Composable
            fun Test(cond: Boolean) {
                if (cond) {
                    A()
                }
                if (cond) {
                    B()
                }
            }
        """
    )

    @Test
    fun testUnusedParameters(): Unit = comparisonPropagation(
        """
            class Unstable(var count: Int)
            class Stable(val count: Int)
            interface MaybeStable
        """,
        """
            @Composable
            fun Unskippable(a: Unstable, b: Stable, c: MaybeStable) {
                used(a)
            }
            @Composable
            fun Skippable1(a: Unstable, b: Stable, c: MaybeStable) {
                used(b)
            }
            @Composable
            fun Skippable2(a: Unstable, b: Stable, c: MaybeStable) {
                used(c)
            }
            @Composable
            fun Skippable3(a: Unstable, b: Stable, c: MaybeStable) { }
        """
    )

    @Test
    fun testExtensionReceiver(): Unit = comparisonPropagation(
        """
            interface MaybeStable
        """,
        """
            @Composable fun MaybeStable.example(x: Int) {
                used(this)
                used(x)
            }
            val example: @Composable MaybeStable.(Int) -> Unit = {
                used(this)
                used(it)
            }
        """
    )

    @Test
    fun testArrayDefaultArgWithState(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.MutableState

            @Composable
            fun VarargComposable(state: MutableState<Int>, vararg values: String = Array(1) { "value " + it }) {
                state.value
            }
        """
    )

    @Test // regression test for 204897513
    fun test_InlineForLoop() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test() {
                Bug(listOf(1, 2, 3)) {
                    Text(it.toString())
                }
            }

            @Composable
            inline fun <T> Bug(items: List<T>, content: @Composable (item: T) -> Unit) {
                for (item in items) content(item)
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) {}
        """
    )
}

class FunctionBodySkippingTransformTestsNoSource(
    useFir: Boolean
) : FunctionBodySkippingTransformTestsBase(useFir) {
    override fun CompilerConfiguration.updateConfiguration() {
        put(ComposeConfiguration.SOURCE_INFORMATION_ENABLED_KEY, false)
        put(ComposeConfiguration.TRACE_MARKERS_ENABLED_KEY, false)
    }

    @Test
    fun testGrouplessProperty(): Unit = comparisonPropagation(
        """
        """,
        """
            import androidx.compose.runtime.currentComposer

            open class Foo {
                inline val current: Int
                    @Composable
                    @ReadOnlyComposable get() = currentComposer.hashCode()

                @ReadOnlyComposable
                @Composable
                fun getHashCode(): Int = currentComposer.hashCode()
            }

            @ReadOnlyComposable
            @Composable
            fun getHashCode(): Int = currentComposer.hashCode()
        """
    )

    @Test // regression test for 204897513
    fun test_InlineForLoop_no_source_info() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            private fun Test() {
                Bug(listOf(1, 2, 3)) {
                    Text(it.toString())
                }
            }

            @Composable
            private inline fun <T> Bug(items: List<T>, content: @Composable (item: T) -> Unit) {
                for (item in items) content(item)
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            fun Text(value: String) {}
        """
    )

    @Test
    fun test_InlineSkipping() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test() {
                InlineWrapperParam {
                    Text("Function ${'$'}it")
                }
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable
            inline fun InlineWrapperParam(content: @Composable (Int) -> Unit) {
                content(100)
            }

            @Composable
            fun Text(text: String) { }
        """
    )

    @Test
    fun test_ComposableLambdaWithUnusedParameter() = verifyGoldenComposeIrTransform(
        source = """
            import androidx.compose.runtime.*

            val layoutLambda = @Composable { _: Int ->
                Layout()
            }
        """,
        extra = """
            import androidx.compose.runtime.*

            @Composable inline fun Layout() {}
        """
    )

    @Test
    fun testNonSkippableComposable() = comparisonPropagation(
        "",
        """
            import androidx.compose.runtime.NonSkippableComposable

            @Composable
            @NonSkippableComposable
            fun Test(i: Int) {
                used(i)
            }
        """.trimIndent()
    )

    @Test
    fun testComposable() = verifyGoldenComposeIrTransform(
        source = """
            interface NewProfileOBViewModel {
                fun overrideMe(): @Type () -> Unit
            }

            class ReturningProfileObViewModel : NewProfileOBViewModel {
                override fun overrideMe(): @Type () -> Unit = {}
            }

            @Target(AnnotationTarget.TYPE)
            annotation class Type
        """
    )
}
