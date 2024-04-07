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

class DefaultParamTransformTests(useFir: Boolean) : AbstractIrTransformTest(useFir) {
    private fun defaultParams(
        @Language("kotlin")
        unchecked: String,
        @Language("kotlin")
        checked: String,
        dumpTree: Boolean = false
    ) = verifyGoldenComposeIrTransform(
        """
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            $checked
        """.trimIndent(),
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
        """
    )

    @Test
    fun testUnusedDefaultComposableLambda(): Unit = defaultParams(
        """
        """,
        """
            inline fun Bar(unused: @Composable () -> Unit = { }) {}
            fun Foo() { Bar() }
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
        """
    )
}
