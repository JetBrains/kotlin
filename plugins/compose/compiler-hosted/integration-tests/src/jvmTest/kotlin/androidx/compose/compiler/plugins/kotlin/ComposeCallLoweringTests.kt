/*
 * Copyright 2024 The Android Open Source Project
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

import org.junit.Ignore
import org.junit.Test

class ComposeCallLoweringTests(useFir: Boolean) : AbstractCodegenTest(useFir) {

    @Test
    fun testVarargs() {
        codegen(
            """
            import androidx.compose.runtime.*

            @Immutable class Foo

            @Composable
            fun A(vararg values: Foo) {
                print(values)
            }

            @Composable
            fun B(vararg values: Int) {
                print(values)
            }
            """
        )
    }

    @Test
    fun testComposableLambdaCall() {
        codegen(
            """
                import androidx.compose.runtime.*

                @Composable
                fun test(content: @Composable () -> Unit) {
                    content()
                }
            """
        )
    }

    @Test
    fun testProperties() {
        codegen(
            """
            import androidx.compose.runtime.*

            val foo @Composable get() = 123

            class A {
                val bar @Composable get() = 123
            }

            val A.bam @Composable get() = 123

            @Composable fun Foo() {
            }

            @Composable
            fun test() {
                val a = A()
                foo
                Foo()
                a.bar
                a.bam
            }
        """
        )
    }

    @Ignore("ui/foundation dependency is not supported for now")
    @Test
    fun testUnboundSymbolIssue() {
        codegenNoImports(
            """
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.layout.Row

            class TodoItem

            @Composable
            fun TodoItemInlineEditor(
                item: TodoItem,
                onEditItemChange: (TodoItem) -> Unit,
                onEditDone: () -> Unit,
                buttonSlot: @Composable() () -> Unit
            ) {}

            @Composable
            fun TodoItemInput(
                text: String,
                onTextChange: (String) -> Unit,
                icon: ImageVector,
                onIconChange: (ImageVector) -> Unit,
                primaryAction: () -> Unit,
                iconsVisible: Boolean,
                modifier: Modifier = Modifier,
                buttonSlot: @Composable() () -> Unit,
            ) {}

            @Composable
            private fun InputTextAndButton(
                text: String,
                onTextChange: (String) -> Unit,
                primaryAction: () -> Unit,
                buttonSlot: @Composable() () -> Unit,
                modifier: Modifier = Modifier
            ) {
                val currentlyEditing = TodoItem()
                TodoItemInlineEditor(
                    item = currentlyEditing,
                    onEditItemChange = {},
                    onEditDone = {},
                    buttonSlot = {
                        Row {
                        }
                    }
                )
            }
            """
        )
    }

    @Test
    fun testComposableLambdaCallWithGenerics() {
        codegen(
            """
                import androidx.compose.runtime.*

                @Composable fun <T> A(value: T, block: @Composable (T) -> Unit) {
                    block(value)
                }

                @Composable fun <T> B(
                    value: T,
                    block: @Composable (@Composable (T) -> Unit) -> Unit
                ) {
                    block({ })
                }

                @Composable
                fun test() {
                    A(123) { it ->
                        println(it)
                    }
                    B(123) { it ->
                        it(456)
                    }
                }
            """
        )
    }

    @Test
    fun testMethodInvocations() {
        codegen(
            """
                import androidx.compose.runtime.*

                val x = compositionLocalOf<Int> { 123 }

                @Composable
                fun test() {
                    CompositionLocalProvider(x provides 456) {

                    }
                }
            """
        )
    }

    @Test
    fun testReceiverLambdaInvocation() {
        codegen(
            """
                class TextSpanScope

                @Composable fun TextSpanScope.Foo(content: @Composable TextSpanScope.() -> Unit) {
                    content()
                }
            """
        )
    }

    @Test
    fun testReceiverLambda2() {
        codegen(
            """
                class DensityScope(val density: Density)

                class Density

                val LocalDensity = compositionLocalOf<Density> { TODO() }

                @Composable
                fun compositionLocalDensity() = LocalDensity.current

                @Composable
                fun WithDensity(block: @Composable DensityScope.() -> Unit) {
                    DensityScope(compositionLocalDensity()).block()
                }
            """
        )
    }

    @Test
    fun testInlineChildren() {
        codegen(
            """
                import androidx.compose.runtime.*

                @Composable
                inline fun PointerInputWrapper(
                    crossinline content: @Composable () -> Unit
                ) {
                    // Hide the internals of PointerInputNode
                    LinearLayout {
                        content()
                    }
                }
            """
        )
    }

    @Test
    fun testNoComposerImport() {
        codegenNoImports(
            """
        import androidx.compose.runtime.Composable

        @Composable fun Wrap(content: @Composable () -> Unit) { content() }

        @Composable
        fun Foo() {
            Wrap {
                // nested calls work
                Bar()
            }
            // calls work
            Bar()
        }

        @Composable
        fun Bar() {}
            """.trimIndent()
        )
    }

    @Test
    fun testInlineNoinline() {
        codegen(
            """
        @Composable
        inline fun PointerInputWrapper(
            crossinline content: @Composable () -> Unit
        ) {
            LinearLayout {
                content()
            }
        }

        @Composable
        fun PressReleasedGestureDetector(content: @Composable () -> Unit) {
            PointerInputWrapper {
                content()
            }
        }
            """.trimIndent()
        )
    }

    @Test
    fun testInlinedComposable() {
        codegen(
            """
        @Composable
        inline fun Foo(crossinline content: @Composable () -> Unit) {
                content()
        }

        @Composable fun test(content: @Composable () -> Unit) {
            Foo {
                println("hello world")
                content()
            }
        }
            """
        )
    }

    @Test
    fun testGenericParameterOrderIssue() {
        codegen(
            """
@Composable
fun A() {
    val x = ""
    val y = ""

    B(bar = x, foo = y)
}

@Composable
fun <T> B(foo: T, bar: String) { }
            """
        )
    }

    @Test
    fun testArgumentOrderIssue() {
        codegen(
            """
                class A

                @Composable
                fun B() {
                    C(bar = 1, foo = 1f)
                }

                @Composable
                fun C(
                    foo: Float,
                    bar: Int
                ) {

                }
            """
        )
    }

    @Test
    fun testObjectName() {
        codegen(
            """

            @Composable fun SomeThing(content: @Composable () -> Unit) {}

            @Composable
            fun Example() {
                SomeThing {
                    val id = object {}
                }
            }
            """
        )
    }

    @Test
    fun testStuffThatIWantTo() {
        codegen(
            """

            fun startCompose(block: @Composable () -> Unit) {}

            fun nonComposable() {
                startCompose {
                    LinearLayout {

                    }
                }
            }
            """
        )
    }

    @Test
    fun testSetContent() {
        codegen(
            """
                fun fakeCompose(block: @Composable ()->Unit) { }

                class Test {
                    fun test() {
                        fakeCompose {
                            LinearLayout {}
                        }
                    }
                }
            """
        )
    }

    @Test
    fun testInlineClassesAsComposableParameters() {
        codegen(
            """
                inline class WrappedInt(val int: Int)

                @Composable fun Pass(wrappedInt: WrappedInt) {
                  wrappedInt.int
                }

                @Composable fun Bar() {
                  Pass(WrappedInt(1))
                }
            """
        )
    }

    @Test
    fun testForDevelopment() {
        codegen(
            """
            import androidx.compose.runtime.*

            @Composable
            fun bar() {

            }
            """
        )
    }

    fun codegen(text: String, dumpClasses: Boolean = false) {
        codegenNoImports(
            """
           import androidx.compose.runtime.*

           $text

           @Composable fun LinearLayout(block: @Composable ()->Unit) { }
        """,
            dumpClasses
        )
    }

    fun codegenNoImports(text: String, dumpClasses: Boolean = false) {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }
}
