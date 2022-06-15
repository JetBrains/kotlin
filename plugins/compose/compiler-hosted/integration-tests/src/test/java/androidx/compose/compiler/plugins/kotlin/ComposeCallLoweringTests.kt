/*
 * Copyright 2019 The Android Open Source Project
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

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ComposeCallLoweringTests : AbstractLoweringTests() {

    @Test
    @Ignore("b/173733968")
    fun testInlineGroups(): Unit = ensureSetup {
        compose(
            """

            @Composable
            fun App() {
                val cond = remember { mutableStateOf(true) }
                val text = if (cond.value) remember { "abc" } else remember { "def" }
                Button(id=1, text=text, onClick={ cond.value = !cond.value })
            }
        """,
            "App()"
        ).then { activity ->
            val tv = activity.findViewById<Button>(1)
            assertEquals("abc", tv.text)
            tv.performClick()
        }.then { activity ->
            val tv = activity.findViewById<Button>(1)
            assertEquals("def", tv.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testReturnInsideKey(): Unit = ensureSetup {
        compose(
            """
            @Composable fun ShowMessage(text: String): Int = key(text) {
                TextView(id=123, text=text)
                return text.length
            }

            @Composable fun App() {
                val length = ShowMessage("hello")
            }
        """,
            "App()"
        ).then { activity ->
            val tv = activity.findViewById<TextView>(123)
            assertEquals("hello", tv.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testMoveFromIssue(): Unit = ensureSetup {
        compose(
            """
        """,
            "Button(id=1, onClick=invalidate)"
        ).then { activity ->
            val tv = activity.findViewById<Button>(1)
            tv.performClick()
        }.then { }
    }

    @Test
    @Ignore("b/173733968")
    fun testSimpleInlining(): Unit = ensureSetup {
        compose(
            """
            @Composable
            inline fun foo(block: @Composable () -> Unit) {
                block()
            }

            @Composable
            fun App() {
                foo {}
            }
        """,
            "App()"
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testVarargCall(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun <T : Any> foo(
                vararg inputs: Any?,
                key: String? = null,
                init: () -> T
            ): T {
                for (input in inputs) {
                    print(input)
                }
                return init()
            }

            @Composable
            fun App() {
                val x = foo { "hello" }
                val y = foo(1, 2) { "world" }
            }
        """,
            "App()"
        ).then {
            // we are only checking that this call successfully completes without throwing
            // an exception
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testVarargs(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testComposableLambdaCall(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testProperties(): Unit = ensureSetup {
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

    @Test
    @Ignore("b/173733968")
    fun testUnboundSymbolIssue(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testPropertyValues(): Unit = ensureSetup {
        compose(
            """
            val foo @Composable get() = "123"

            class A {
                val bar @Composable get() = "123"
            }

            val A.bam @Composable get() = "123"

            @Composable
            fun App() {
                val a = A()
                TextView(id=1, text=a.bar)
                TextView(id=2, text=foo)
                TextView(id=3, text=a.bam)
            }
        """,
            "App()"
        ).then { activity ->
            fun assertText(id: Int, value: String) {
                val tv = activity.findViewById<TextView>(id)
                assertEquals(value, tv.text)
            }
            assertText(1, "123")
            assertText(2, "123")
            assertText(3, "123")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testComposableLambdaCallWithGenerics(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testMethodInvocations(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testReceiverLambdaInvocation(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testReceiverLambda2(): Unit = ensureSetup {
        codegen(
            """
                class DensityScope(val density: Density)

                class Density

                val LocalDensity = compositionLocalOf<Density>()

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
    @Ignore("b/173733968")
    fun testInlineChildren(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testNoComposerImport(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testInlineNoinline(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testInlinedComposable(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testGenericParameterOrderIssue(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testArgumentOrderIssue(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testObjectName(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testStuffThatIWantTo(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testSimpleFunctionResolution(): Unit = ensureSetup {
        compose(
            """
            import androidx.compose.runtime.*

            @Composable
            fun noise(text: String) {}

            @Composable
            fun bar() {
                noise(text="Hello World")
            }
            """,
            """
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testSimpleClassResolution(): Unit = ensureSetup {
        compose(
            """
            import androidx.compose.runtime.*

            @Composable
            fun bar() {
                TextView(text="Hello World")
            }
            """,
            """
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testSetContent(): Unit = ensureSetup {
        codegen(
            """
                fun fakeCompose(block: @Composable ()->Unit) { }

                class Test {
                    fun test() {
                        fakeCompose {
                            LinearLayout(orientation = LinearLayout.VERTICAL) {}
                        }
                    }
                }
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testComposeWithResult(): Unit = ensureSetup {
        compose(
            """
                @Composable fun <T> identity(block: @Composable ()->T): T = block()

                @Composable
                fun TestCall() {
                  val value: Any = identity { 12 }
                  TextView(text = value.toString(), id = 100)
                }
            """,
            "TestCall()"
        ).then { activity ->
            val textView = activity.findViewById<TextView>(100)
            assertEquals("12", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservable(): Unit = ensureSetup {
        compose(
            """
                import androidx.compose.runtime.*

                @Composable
                fun SimpleComposable() {
                    FancyButton(state=mutableStateOf(0))
                }

                @Composable
                fun FancyButton(state: MutableState<Int>) {
                    Button(text=("Clicked "+state.value+" times"), onClick={state.value++}, id=42)
                }
            """,
            "SimpleComposable()"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Clicked 3 times", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableLambda(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun SimpleComposable(state: MutableState<Int>) {
                    FancyBox2 {
                        Button(
                          text=("Button clicked "+state.value+" times"),
                          onClick={state.value++},
                          id=42
                        )
                    }
                }

                @Composable
                fun FancyBox2(content: @Composable ()->Unit) {
                    content()
                }
            """,
            "SimpleComposable(state=remember { mutableStateOf(0) })"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableGenericFunction(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun <T> SimpleComposable(state: MutableState<Int>, value: T) {
                Button(
                  text=("Button clicked "+state.value+" times: " + value),
                  onClick={state.value++},
                  id=42
                )
            }
        """,
            "SimpleComposable(state=remember { mutableStateOf(0) }, value=\"Value\")"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times: Value", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableExtension(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun MutableState<Int>.Composable() {
                Button(
                    text="Button clicked "+value+" times",
                    onClick={value++},
                    id=42
                )
            }

            val myCounter = mutableStateOf(0)
            """,
            "myCounter.Composable()"
        ).then { activity ->
            val button = activity.findViewById<Button>(42)
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObserverableExpressionBody(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun SimpleComposable(counter: MutableState<Int>) =
                Button(
                    text="Button clicked "+counter.value+" times",
                    onClick={counter.value++},
                    id=42
                )

            @Composable
            fun SimpleWrapper(counter: MutableState<Int>) = SimpleComposable(counter = counter)

            val myCounter = mutableStateOf(0)
            """,
            "SimpleWrapper(counter = myCounter)"
        ).then { activity ->
            val button = activity.findViewById<Button>(42)
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableInlineWrapper(): Unit = ensureSetup {
        compose(
            """
            var inWrapper = false
            val counter = mutableStateOf(0)

            inline fun wrapper(block: () -> Unit) {
              inWrapper = true
              try {
                block()
              } finally {
                inWrapper = false
              }
            }

            @Composable
            fun SimpleComposable(state: MutableState<Int>) {
                wrapper {
                    Button(
                      text=("Button clicked "+state.value+" times"),
                      onClick={state.value++},
                      id=42
                    )
                }
            }
        """,
            "SimpleComposable(state=counter)"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("Button clicked 3 times", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableDefaultParameter(): Unit = ensureSetup {
        compose(
            """
            val counter = mutableStateOf(0)

            @Composable
            fun SimpleComposable(state: MutableState<Int>, a: Int = 1, b: Int = 2) {
                Button(
                  text=("State: ${'$'}{state.value} a = ${'$'}a b = ${'$'}b"),
                  onClick={state.value++},
                  id=42
                )
            }
        """,
            "SimpleComposable(state=counter, b = 4)"
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals("State: 3 a = 1 b = 4", button.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObservableEarlyReturn(): Unit = ensureSetup {
        compose(
            """
            val counter = mutableStateOf(0)

            @Composable
            fun SimpleComposable(state: MutableState<Int>) {
                Button(
                  text=("State: ${'$'}{state.value}"),
                  onClick={state.value++},
                  id=42
                )

                if (state.value > 2) return

                TextView(
                  text="Included text",
                  id=43
                )
            }
        """,
            "SimpleComposable(state=counter)"
        ).then { activity ->
            // Check that the text view is in the view
            assertNotNull(activity.findViewById(43))
            val button = activity.findViewById(42) as Button
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById<Button>(42)
            assertEquals("State: 3", button.text)

            // Assert that the text view is no longer in the view
            assertNull(activity.findViewById<Button>(43))
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGSimpleTextView(): Unit = ensureSetup {
        compose(
            """

            """,
            """
                TextView(text="Hello, world!", id=42)
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGLocallyScopedFunction(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo() {
                    @Composable fun Bar() {
                        TextView(text="Hello, world!", id=42)
                    }
                    Bar()
                }
            """,
            """
                Foo()
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGLocallyScopedExtensionFunction(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(x: String) {
                    @Composable fun String.Bar() {
                        TextView(text=this, id=42)
                    }
                    x.Bar()
                }
            """,
            """
                Foo(x="Hello, world!")
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testImplicitReceiverScopeCall(): Unit = ensureSetup {
        compose(
            """
                import androidx.compose.runtime.*

                class Bar(val text: String)

                @Composable fun Bar.Foo() {
                    TextView(text=text,id=42)
                }

                @Composable
                fun Bam(bar: Bar) {
                    with(bar) {
                        Foo()
                    }
                }
            """,
            """
                Bam(bar=Bar("Hello, world!"))
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGLocallyScopedInvokeOperator(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(x: String) {
                    @Composable
                    operator fun String.invoke() {
                        TextView(text=this, id=42)
                    }
                    x()
                }
            """,
            """
                Foo(x="Hello, world!")
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testTrivialExtensionFunction(): Unit = ensureSetup {
        compose(
            """ """,
            """
                val x = "Hello"
                @Composable fun String.foo() {}
                x.foo()
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testTrivialInvokeExtensionFunction(): Unit = ensureSetup {
        compose(
            """ """,
            """
                val x = "Hello"
                @Composable operator fun String.invoke() {}
                x()
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNSimpleTextView(): Unit = ensureSetup {
        compose(
            """

            """,
            """
                TextView(text="Hello, world!", id=42)
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testInliningTemp2(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(onClick: Double.() -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then { }
    }

    @Test
    @Ignore("b/173733968")
    fun testInliningTemp3(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(onClick: (Double) -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then { }
    }

    @Test
    @Ignore("b/173733968")
    fun testInliningTemp4(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(onClick: (Double) -> Unit) {

                }
            """,
            """
                Foo(onClick={})
            """,
            { mapOf("foo" to "bar") }
        ).then {}
    }

    @Test
    @Ignore("b/173733968")
    fun testInline_NonComposable_Identity(): Unit = ensureSetup {
        compose(
            """
            @Composable inline fun InlineWrapper(base: Int, content: @Composable ()->Unit) {
              content()
            }
            """,
            """
            InlineWrapper(200) {
              TextView(text = "Test", id=101)
            }
            """
        ).then { activity ->
            assertEquals("Test", activity.findViewById<TextView>(101).text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testInline_Composable_Identity(): Unit = ensureSetup {
        compose(
            """
            """,
            """
              TextView(text="Test", id=101)
            """
        ).then { activity ->
            assertEquals("Test", activity.findViewById<TextView>(101).text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testInline_Composable_EmitChildren(): Unit = ensureSetup {
        compose(
            """
            @Composable
            inline fun InlineWrapper(base: Int, crossinline content: @Composable ()->Unit) {
              LinearLayout(id = base + 0) {
                content()
              }
            }
            """,
            """
            InlineWrapper(200) {
              TextView(text = "Test", id=101)
            }
            """
        ).then { activity ->
            val tv = activity.findViewById<TextView>(101)
            // Assert the TextView was created with the correct text
            assertEquals("Test", tv.text)
            // and it is the first child of the linear layout
            assertEquals(tv, activity.findViewById<LinearLayout>(200).getChildAt(0))
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNInlining(): Unit = ensureSetup {
        compose(
            """

            """,
            """
                LinearLayout(orientation=LinearLayout.VERTICAL) {
                    TextView(text="Hello, world!", id=42)
                }
            """
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testInlineClassesAsComposableParameters(): Unit = ensureSetup {
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
    @Ignore("b/173733968")
    fun testInlineClassesAsDefaultParameters(): Unit = ensureSetup {
        compose(
            """
                inline class Positive(val int: Int) {
                  init { require(int > 0) }
                }

                @Composable fun Check(positive: Positive = Positive(1)) {
                  positive.int
                }
            """,
            "Check()",
            noParameters
        ).then {
            // Everything is fine if we get here without an exception.
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testRangeForLoop(): Unit = ensureSetup {
        codegen(
            """
                @Composable fun Foo(i: Int) {}
                @Composable
                fun Bar(items: Array<Int>) {
                  for (i in items) {
                    Foo(i)
                  }
                }
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testReturnValue(): Unit = ensureSetup {
        compose(
            """
            var a = 0
            var b = 0

            @Composable
            fun SimpleComposable() {
                a++
                val c = remember { mutableStateOf(0) }
                val d = remember(c.value) { b++; b }
                val scope = currentRecomposeScope
                Button(
                  text=listOf(a, b, c.value, d).joinToString(", "),
                  onClick={ c.value += 1 },
                  id=42
                )
                Button(
                  text="Recompose",
                  onClick={ scope.invalidate() },
                  id=43
                )
            }
        """,
            "SimpleComposable()",
            noParameters
        ).then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals(
                button.text,
                listOf(
                    1, // SimpleComposable has run once
                    1, // memo has been called once because of initial mount
                    0, // state was in itialized at 0
                    1 // memo should return b
                ).joinToString(", ")
            )
            button.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            val recompose = activity.findViewById(43) as Button
            assertEquals(
                button.text,
                listOf(
                    2, // SimpleComposable has run twice
                    2, // memo has been called twice, because state input has changed
                    1, // state was changed to 1
                    2 // memo should return b
                ).joinToString(", ")
            )
            recompose.performClick()
        }.then { activity ->
            val button = activity.findViewById(42) as Button
            assertEquals(
                button.text,
                listOf(
                    3, // SimpleComposable has run three times
                    2, // memo was not called this time, because input didn't change
                    1, // state stayed at 1
                    2 // memo should return b
                ).joinToString(", ")
            )
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testReorderedArgsReturnValue(): Unit = ensureSetup {
        compose(
            """
            @Composable
            fun SimpleComposable() {
                val x = remember(calculation = { "abc" }, v1 = "def")
                TextView(
                  text=x,
                  id=42
                )
            }
        """,
            "SimpleComposable()",
            noParameters
        ).then { activity ->
            val button = activity.findViewById(42) as TextView
            assertEquals(button.text, "abc")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testTrivialReturnValue(): Unit = ensureSetup {
        compose(
            """
        @Composable
        fun <T> identity(value: T): T = value

        @Composable
        fun SimpleComposable() {
            val x = identity("def")
            TextView(
              text=x,
              id=42
            )
        }
    """,
            "SimpleComposable()",
            noParameters
        ).then { activity ->
            val button = activity.findViewById(42) as TextView
            assertEquals(button.text, "def")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testForDevelopment(): Unit = ensureSetup {
        codegen(
            """
            import androidx.compose.runtime.*

            @Composable
            fun bar() {

            }

            @Composable
            fun foo() {
                TextView(text="Hello World")
            }
            """
        )
    }

    @Test
    @Ignore("b/173733968")
    fun testInliningTemp(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(x: Double, content: @Composable Double.() -> Unit) {
                  x.content()
                }
            """,
            """
                Foo(x=1.0) {
                    TextView(text=this.toString(), id=123)
                }
            """,
            { mapOf("foo" to "bar") }
        ).then { activity ->
            val textView = activity.findViewById(123) as TextView
            assertEquals("1.0", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGUpdatedComposition(): Unit = ensureSetup {
        var value = "Hello, world!"

        compose(
            """""",
            """
               TextView(text=value, id=42)
            """,
            { mapOf("value" to value) }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)

            value = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Other value", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNUpdatedComposition(): Unit = ensureSetup {
        var value = "Hello, world!"

        compose(
            """""",
            """
               TextView(text=value, id=42)
            """,
            { mapOf("value" to value) }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)

            value = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Other value", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGViewGroup(): Unit = ensureSetup {
        val tvId = 258
        val llId = 260
        var text = "Hello, world!"
        var orientation = LinearLayout.HORIZONTAL

        compose(
            """""",
            """
                LinearLayout(orientation=orientation, id=$llId) {
                  TextView(text=text, id=$tvId)
                }
            """,
            { mapOf("text" to text, "orientation" to orientation) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)

            text = "Other value"
            orientation = LinearLayout.VERTICAL
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNFunctionComponent(): Unit = ensureSetup {
        var text = "Hello, world!"
        val tvId = 123

        compose(
            """
            @Composable
            fun Foo(text: String) {
                TextView(id=$tvId, text=text)
            }

        """,
            """
             Foo(text=text)
        """,
            { mapOf("text" to text) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
            text = "wat"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCompositionLocalConsumedFromDefaultParameter(): Unit = ensureSetup {
        val initialText = "no text"
        val helloWorld = "Hello World!"
        compose(
            """
            val LocalText = compositionLocalOf { "$initialText" }

            @Composable
            fun Main() {
                var text = remember { mutableStateOf("$initialText") }
                CompositionLocalProvider(LocalText provides text.value) {
                    LinearLayout {
                        ConsumesCompositionLocalFromDefaultParameter()
                        Button(
                            text = "Change CompositionLocal value",
                            onClick={ text.value = "$helloWorld" },
                            id=101
                        )
                    }
                }
            }

            @Composable
            fun ConsumesCompositionLocalFromDefaultParameter(text: String = LocalText.current) {
                TextView(text = text, id = 42)
            }
        """,
            "Main()",
            noParameters
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals(initialText, textView.text)
        }.then { activity ->
            val button = activity.findViewById(101) as Button
            button.performClick()
        }
            .then { activity ->
                val textView = activity.findViewById(42) as TextView
                assertEquals(helloWorld, textView.text)
            }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNViewGroup(): Unit = ensureSetup {
        val tvId = 258
        val llId = 260
        var text = "Hello, world!"
        var orientation = LinearLayout.HORIZONTAL

        compose(
            """""",
            """
                 LinearLayout(orientation=orientation, id=$llId) {
                   TextView(text=text, id=$tvId)
                 }
            """,
            { mapOf("text" to text, "orientation" to orientation) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)

            text = "Other value"
            orientation = LinearLayout.VERTICAL
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val linearLayout = activity.findViewById(llId) as LinearLayout

            assertEquals(text, textView.text)
            assertEquals(orientation, linearLayout.orientation)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testMemoization(): Unit = ensureSetup {
        val tvId = 258
        val tagId = (3 shl 24) or "composed_set".hashCode()

        compose(
            """
                var composedSet = mutableSetOf<String>()
                var inc = 1

                @Composable fun ComposedTextView(id: Int, composed: Set<String>) {
                  emitView(::TextView) {
                    it.id = id
                    it.setTag($tagId, composed)
                  }
                }

                @Composable fun ComposePrimitive(value: Int) {
                    composedSet.add("ComposePrimitive(" + value + ")")
                }

                class MutableThing(var value: String)

                val constantMutableThing = MutableThing("const")

                @Composable fun ComposeMutable(value: MutableThing) {
                    composedSet.add("ComposeMutable(" + value.value + ")")
                }
            """,
            """
                composedSet.clear()

                ComposePrimitive(value=123)
                ComposePrimitive(value=inc)
                ComposeMutable(value=constantMutableThing)
                ComposeMutable(value=MutableThing("new"))

                ComposedTextView(id=$tvId, composed=composedSet)

                inc++
            """,
            { mapOf("text" to "") }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId) ?: error(
                "expected a compose set to exist"
            )

            fun assertContains(contains: Boolean, key: String) {
                assertEquals("composedSet contains key '$key'", contains, composedSet.contains(key))
            }

            assertContains(true, "ComposePrimitive(123)")
            assertContains(true, "ComposePrimitive(1)")
            assertContains(true, "ComposeMutable(const)")
            assertContains(true, "ComposeMutable(new)")
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId)
                ?: error("expected a compose set to exist")

            fun assertContains(contains: Boolean, key: String) {
                assertEquals("composedSet contains key '$key'", contains, composedSet.contains(key))
            }

            // the primitive component skips based on equality
            assertContains(false, "ComposePrimitive(123)")

            // since the primitive changed, this one recomposes again
            assertContains(true, "ComposePrimitive(2)")

            // since this is a potentially mutable object, we don't skip based on it
            assertContains(true, "ComposeMutable(const)")

            // since it's a new one every time, we definitely don't skip
            assertContains(true, "ComposeMutable(new)")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testInlineClassMemoization(): Unit = ensureSetup {
        val tvId = 258
        val tagId = (3 shl 24) or "composed_set".hashCode()

        compose(
            """
                inline class InlineInt(val value: Int)
                inline class InlineInlineInt(val value: InlineInt)
                inline class InlineMutableSet(val value: MutableSet<String>)
                @Composable fun ComposedTextView(id: Int, composed: Set<String>) {
                  emitView(::TextView) {
                    it.id = id
                    it.setTag($tagId, composed)
                  }
                }

                val composedSet = mutableSetOf<String>()
                val constInlineInt = InlineInt(0)
                var inc = 2
                val constInlineMutableSet = InlineMutableSet(mutableSetOf("a"))

                @Composable fun ComposedInlineInt(value: InlineInt) {
                  composedSet.add("ComposedInlineInt(" + value + ")")
                }

                @Composable fun ComposedInlineInlineInt(value: InlineInlineInt) {
                  composedSet.add("ComposedInlineInlineInt(" + value + ")")
                }

                @Composable fun ComposedInlineMutableSet(value: InlineMutableSet) {
                  composedSet.add("ComposedInlineMutableSet(" + value + ")")
                }
            """,
            """
                composedSet.clear()

                ComposedInlineInt(constInlineInt)
                ComposedInlineInt(InlineInt(1))
                ComposedInlineInt(InlineInt(inc))
                ComposedInlineInlineInt(InlineInlineInt(InlineInt(2)))
                ComposedInlineMutableSet(constInlineMutableSet)

                ComposedTextView(id=$tvId, composed=composedSet)

                inc++
            """,
            { mapOf("text" to "") }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId)
                ?: error("expected a compose set to exist")

            // All composables should execute since it's the first time.
            assert(composedSet.contains("ComposedInlineInt(InlineInt(value=0))"))
            assert(composedSet.contains("ComposedInlineInt(InlineInt(value=1))"))
            assert(composedSet.contains("ComposedInlineInt(InlineInt(value=2))"))
            assert(
                composedSet.contains(
                    "ComposedInlineInlineInt(InlineInlineInt(value=InlineInt(value=2)))"
                )
            )
            assert(composedSet.contains("ComposedInlineMutableSet(InlineMutableSet(value=[a]))"))
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId)
                ?: error("expected a compose set to exist")

            // InlineInt and InlineInlineInt are stable, so the corresponding composables should
            // not run for values equal to previous compositions.
            assert(!composedSet.contains("ComposedInlineInt(InlineInt(value=0))"))
            assert(!composedSet.contains("ComposedInlineInt(InlineInt(value=1))"))
            assert(
                !composedSet.contains(
                    "ComposedInlineInlineInt(InlineInlineInt(value=InlineInt(value=2)))"
                )
            )

            // But if a stable composable is passed a new value, it should re-run.
            assert(composedSet.contains("ComposedInlineInt(InlineInt(value=3))"))

            // And composables for inline classes with non-stable underlying types should run.
            assert(composedSet.contains("ComposedInlineMutableSet(InlineMutableSet(value=[a]))"))
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testStringParameterMemoization(): Unit = ensureSetup {
        val tvId = 258
        val tagId = (3 shl 24) or "composed_set".hashCode()

        compose(
            """
                @Composable fun ComposedTextView(id: Int, composed: Set<String>) {
                  emitView(::TextView) {
                    it.id = id
                    it.setTag($tagId, composed)
                  }
                }

                val composedSet = mutableSetOf<String>()
                val FOO = "foo"

                @Composable fun ComposedString(value: String) {
                  composedSet.add("ComposedString(" + value + ")")
                }
            """,
            """
                composedSet.clear()

                ComposedString(FOO)
                ComposedString("bar")

                ComposedTextView(id=$tvId, composed=composedSet)
            """,
            { mapOf("text" to "") }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId)
                ?: error("expected a compose set to exist")

            // All composables should execute since it's the first time.
            assert(composedSet.contains("ComposedString(foo)"))
            assert(composedSet.contains("ComposedString(bar)"))
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            val composedSet = textView.getComposedSet(tagId)
                ?: error("expected a compose set to exist")

            assert(composedSet.isEmpty())
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNSimpleCall(): Unit = ensureSetup {
        val tvId = 258
        var text = "Hello, world!"

        compose(
            """
                @Composable fun SomeFun(x: String) {
                    TextView(text=x, id=$tvId)
                }
            """,
            """
                SomeFun(x=text)
            """,
            { mapOf("text" to text) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)

            text = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGNCallWithChildren(): Unit = ensureSetup {
        val tvId = 258
        var text = "Hello, world!"

        compose(
            """
                @Composable
                fun Block(content: @Composable () -> Unit) {
                    content()
                }
            """,
            """
                Block {
                    Block {
                        TextView(text=text, id=$tvId)
                    }
                }
            """,
            { mapOf("text" to text) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)

            text = "Other value"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView

            assertEquals(text, textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGComposableFunctionInvocationOneParameter(): Unit = ensureSetup {
        val tvId = 91
        var phone = "(123) 456-7890"
        compose(
            """
           @Composable
           fun Phone(value: String) {
             TextView(text=value, id=$tvId)
           }
        """,
            """
           Phone(value=phone)
        """,
            { mapOf("phone" to phone) }
        ).then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            assertEquals(phone, textView.text)

            phone = "(123) 456-7899"
        }.then { activity ->
            val textView = activity.findViewById(tvId) as TextView
            assertEquals(phone, textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCGComposableFunctionInvocationTwoParameters(): Unit = ensureSetup {
        val tvId = 111
        val rsId = 112
        var left = 0
        var right = 0
        compose(
            """
           var addCalled = 0

           @Composable
           fun AddView(left: Int, right: Int) {
             addCalled++
             TextView(text="${'$'}left + ${'$'}right = ${'$'}{left + right}", id=$tvId)
             TextView(text="${'$'}addCalled", id=$rsId)
           }
        """,
            """
           AddView(left=left, right=right)
        """,
            { mapOf("left" to left, "right" to right) }
        ).then { activity ->
            // Should be called on the first compose
            assertEquals("1", (activity.findViewById(rsId) as TextView).text)
            assertEquals(
                "$left + $right = ${left + right}",
                (activity.findViewById(tvId) as TextView).text
            )
        }.then { activity ->
            // Should be skipped on the second compose
            assertEquals("1", (activity.findViewById(rsId) as TextView).text)
            assertEquals(
                "$left + $right = ${left + right}",
                (activity.findViewById(tvId) as TextView).text
            )

            left = 1
        }.then { activity ->
            // Should be called again because left changed.
            assertEquals("2", (activity.findViewById(rsId) as TextView).text)
            assertEquals(
                "$left + $right = ${left + right}",
                (activity.findViewById(tvId) as TextView).text
            )

            right = 41
        }.then { activity ->
            // Should be called again because right changed
            assertEquals("3", (activity.findViewById(rsId) as TextView).text)
            assertEquals(
                "$left + $right = ${left + right}",
                (activity.findViewById(tvId) as TextView).text
            )
        }.then { activity ->
            // Should be skipped because nothing changed
            assertEquals("3", (activity.findViewById(rsId) as TextView).text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testImplicitReceiverPassing1(): Unit = ensureSetup {
        compose(
            """
                @Composable fun Int.Foo(x: @Composable Int.() -> Unit) {
                    x()
                }
            """,
            """
                val id = 42

                id.Foo(x={
                    TextView(text="Hello, world!", id=this)
                })
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testImplicitReceiverPassing2(): Unit = ensureSetup {
        compose(
            """
                @Composable fun Int.Foo(x: @Composable Int.(text: String) -> Unit, text: String) {
                    x(text=text)
                }

                @Composable fun MyText(text: String, id: Int) {
                    TextView(text=text, id=id)
                }
            """,
            """
                val id = 42

                id.Foo(text="Hello, world!", x={ text ->
                    MyText(text=text, id=this)
                })
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testEffects1(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Counter() {
                    var count = remember { mutableStateOf(0) }
                    TextView(
                        text=("Count: " + count.value),
                        onClick={
                            count.value += 1
                        },
                        id=42
                    )
                }
            """,
            """
                Counter()
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 0", textView.text)
            textView.performClick()
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 1", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testEffects2(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Counter() {
                    var count = remember { mutableStateOf(0) }
                    TextView(
                        text=("Count: " + count.value),
                        onClick={
                            count.value += 1
                        },
                        id=42
                    )
                }
            """,
            """
                Counter()
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 0", textView.text)
            textView.performClick()
        }.then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 1", textView.text)
        }
    }

    @Ignore("b/171801506")
    @Test
    fun testEffects3(): Unit = ensureSetup {
        val log = StringBuilder()
        compose(
            """
                @Composable
                fun Counter(log: StringBuilder) {
                    var count = remember { mutableStateOf(0) }
                    onCommit {
                        log.append("a")
                    }
                    onActive {
                        log.append("b")
                    }
                    TextView(
                        text=("Count: " + count.value),
                        onClick={
                            count.value += 1
                        },
                        id=42
                    )
                }
            """,
            """
                Counter(log=log)
            """,
            { mapOf("log" to log) }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 0", textView.text)
            assertEquals("ab", log.toString())

            execute {
                textView.performClick()
            }

            assertEquals("Count: 1", textView.text)
            assertEquals("aba", log.toString())
        }
    }

    @Ignore("b/171801506")
    @Test
    fun testEffects4(): Unit = ensureSetup {
        val log = StringBuilder()
        compose(
            """
                @Composable
                fun printer(log: StringBuilder, str: String) {
                    onCommit {
                        log.append(str)
                    }
                }

                @Composable
                fun Counter(log: StringBuilder) {
                    var count = remember { mutableStateOf(0) }
                    printer(log, "" + count.value)
                    TextView(
                        text=("Count: " + count.value),
                        onClick={
                            count.value += 1
                        },
                        id=42
                    )
                }
            """,
            """
                Counter(log=log)
            """,
            { mapOf("log" to log) }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Count: 0", textView.text)
            assertEquals("0", log.toString())

            execute {
                textView.performClick()
            }

            assertEquals("Count: 1", textView.text)
            assertEquals("01", log.toString())
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testVariableCalls1(): Unit = ensureSetup {
        compose(
            """
                val component = @Composable {
                    TextView(text="Hello, world!", id=42)
                }
            """,
            """
                component()
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testVariableCalls2(): Unit = ensureSetup {
        compose(
            """
                val component = @Composable {
                    TextView(text="Hello, world!", id=42)
                }
                class HolderA(val composable: @Composable () -> Unit)

                val holder = HolderA(component)

            """,
            """
                holder.composable()
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testVariableCalls3(): Unit = ensureSetup {
        compose(
            """
                val component = @Composable {
                    TextView(text="Hello, world!", id=42)
                }
                class HolderB(val composable: @Composable () -> Unit) {
                    @Composable
                    fun Foo() {
                        composable()
                    }
                }

                val holder = HolderB(component)

            """,
            """
                holder.Foo()
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    // b/123721921
    @Test
    @Ignore("b/173733968")
    fun testDefaultParameters1(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(a: Int = 42, b: String) {
                    TextView(text=b, id=a)
                }
            """,
            """
                Foo(b="Hello, world!")
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testDefaultParameters2(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun Foo(a: Int = 42, b: String, c: @Composable () -> Unit) {
                    c()
                    TextView(text=b, id=a)
                }
            """,
            """
                Foo(b="Hello, world!") {}
            """,
            { mapOf<String, String>() }
        ).then { activity ->
            val textView = activity.findViewById(42) as TextView
            assertEquals("Hello, world!", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testMovement(): Unit = ensureSetup {
        val tvId = 50
        val btnIdAdd = 100
        val btnIdUp = 200
        val btnIdDown = 300

        // Duplicate the steps to reproduce an issue discovered in the Reorder example
        compose(
            """
            fun <T> List<T>.move(from: Int, to: Int): List<T> {
                if (to < from) return move(to, from)
                val item = get(from)
                val currentItem = get(to)
                val left = if (from > 0) subList(0, from) else emptyList()
                val right = if (to < size) subList(to + 1, size) else emptyList()
                val middle = if (to - from > 1) subList(from + 1, to) else emptyList()
                return left + listOf(currentItem) + middle + listOf(item) + right
            }

            @Composable
            fun Reordering() {
                val items = remember { mutableStateOf(listOf(1, 2, 3, 4, 5)) }

                LinearLayout(orientation=LinearLayout.VERTICAL) {
                    items.value.forEachIndexed { index, id ->
                        key(id) {
                            Item(
                                id=id,
                                onMove={ amount ->
                                    val next = index + amount
                                    if (next >= 0 && next < items.value.size) {
                                        items.value = items.value.move(index, index + amount)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            @Composable
            private fun Item(id: Int, onMove: (Int) -> Unit) {
                val count = remember { mutableStateOf(0) }
                LinearLayout(orientation=LinearLayout.HORIZONTAL) {
                    TextView(id=(id+$tvId), text="id: ${'$'}id amt: ${'$'}{count.value}")
                    Button(id=(id+$btnIdAdd), text="+", onClick={ count.value++ })
                    Button(id=(id+$btnIdUp), text="Up", onClick={ onMove(1) })
                    Button(id=(id+$btnIdDown), text="Down", onClick={ onMove(-1) })
                }
            }
            """,
            """
               Reordering()
            """,
            noParameters
        ).then { activity ->
            // Click 5 add
            val button = activity.findViewById(btnIdAdd + 5) as Button
            button.performClick()
        }.then { activity ->
            // Click 5 down
            val button = activity.findViewById(btnIdDown + 5) as Button
            button.performClick()
        }.then { activity ->
            // Click 5 down
            val button = activity.findViewById(btnIdDown + 5) as Button
            button.performClick()
        }.then { activity ->
            // Click 5 up
            val button = activity.findViewById(btnIdUp + 5) as Button
            button.performClick()
        }.then { activity ->
            // Click 5 up
            val button = activity.findViewById(btnIdUp + 5) as Button
            button.performClick()
        }.then { activity ->
            // Click 5 add
            val button = activity.findViewById(btnIdAdd + 5) as Button
            button.performClick()
        }.then { activity ->
            val textView = activity.findViewById(tvId + 5) as TextView
            assertEquals("id: 5 amt: 2", textView.text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testObserveKtxWithInline(): Unit = ensureSetup {
        compose(
            """
                @Composable
                fun SimpleComposable() {
                    val count = remember { mutableStateOf(1) }
                    Box {
                        repeat(count.value) {
                            Button(text="Increment", onClick={ count.value += 1 }, id=(41+it))
                        }
                    }
                }

                @Composable
                fun Box(content: @Composable ()->Unit) {
                    LinearLayout(orientation=LinearLayout.VERTICAL) {
                        content()
                    }
                }
            """,
            """
               SimpleComposable()
            """,
            noParameters
        ).then { activity ->
            val button = activity.findViewById(41) as Button
            button.performClick()
            button.performClick()
            button.performClick()
            button.performClick()
            button.performClick()
        }.then { activity ->
            assertNotNull(activity.findViewById(46))
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testKeyTag(): Unit = ensureSetup {
        compose(
            """
            val list = mutableStateListOf(0,1,2,3)

            @Composable
            fun Reordering() {
                LinearLayout {
                    Button(
                      id=50,
                      text="Recompose!",
                      onClick={ list.add(list.removeAt(0)); }
                    )
                    LinearLayout(id=100) {
                        for(id in list) {
                            key(id) {
                                StatefulButton()
                            }
                        }
                    }
                }
            }

            @Composable
            private fun StatefulButton() {
                val count = remember { mutableStateOf(0) }
                Button(text="Clicked ${'$'}{count.value} times!", onClick={ count.value++ })
            }
            """,
            """
               Reordering()
            """,
            noParameters
        ).then { activity ->
            val layout = activity.findViewById(100) as LinearLayout
            layout.getChildAt(0).performClick()
        }.then { activity ->
            val recomposeButton = activity.findViewById(50) as Button
            recomposeButton.performClick()
        }.then { activity ->
            val layout = activity.findViewById(100) as LinearLayout
            assertEquals("Clicked 0 times!", (layout.getChildAt(0) as Button).text)
            assertEquals("Clicked 0 times!", (layout.getChildAt(1) as Button).text)
            assertEquals("Clicked 0 times!", (layout.getChildAt(2) as Button).text)
            assertEquals("Clicked 1 times!", (layout.getChildAt(3) as Button).text)
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testNonComposeParameters(): Unit = ensureSetup {
        compose(
            """
                class Action(
                   val s: String = "",
                   val param: Int,
                   type: Set<Int> = setOf(),
                   val action: () -> Unit
                )

                @Composable
                fun DefineAction(
                    onAction: Action = Action(param = 1) {},
                    content: @Composable ()->Unit
                 ) { }
            """,
            """"""
        )
    }

    @Ignore("b/171801506")
    @Test
    fun testStableParameters_Various(): Unit = ensureSetup {
        val output = ArrayList<String>()
        compose(
            """
            val m = mutableStateOf(0)

            @Immutable
            data class ValueHolder(val value: Int)

            var output = ArrayList<String>()

            class NotStable { var value = 10 }

            @Stable
            class StableClass {
                override fun equals(other: Any?) = true
            }

            enum class EnumState {
              One,
              Two
            }

            val mutableStateType = mutableStateOf(1)
            val stateType: State<Int> = mutableStateType

            @Composable
            fun MemoInt(a: Int) {
              output.add("MemoInt a=${'$'}a")
              Button(id=101, text="memo ${'$'}a", onClick={ m.value++ })
            }

            @Composable
            fun MemoFloat(a: Float) {
              output.add("MemoFloat")
              Button(text="memo ${'$'}a")
            }

            @Composable
            fun MemoDouble(a: Double) {
              output.add("MemoDouble")
              Button(text="memo ${'$'}a")
            }

            @Composable
            fun MemoNotStable(a: NotStable) {
              output.add("MemoNotStable")
              Button(text="memo ${'$'}{a.value}")
            }

            @Composable
            fun MemoModel(a: ValueHolder) {
              output.add("MemoModelHolder")
              Button(text="memo ${'$'}{a.value}")
            }

            @Composable
            fun MemoEnum(a: EnumState) {
              output.add("MemoEnum")
              Button(text="memo ${'$'}{a}")
            }

            @Composable
            fun MemoStable(a: StableClass) {
              output.add("MemoStable")
              Button(text="memo stable")
            }

            @Composable
            fun MemoMutableState(a: MutableState<Int>) {
              output.add("MemoMutableState")
              Button(text="memo ${'$'}{a.value}")
            }

            @Composable
            fun MemoState(a: State<Int>) {
              output.add("MemoState")
              Button(text="memo ${'$'}{a.value}")
            }

            @Composable
            fun TestSkipping(
                a: Int,
                b: Float,
                c: Double,
                d: NotStable,
                e: ValueHolder,
                f: EnumState,
                g: StableClass,
                h: MutableState<Int>,
                i: State<Int>
            ) {
              val am = a + m.value
              output.add("TestSkipping a=${'$'}a am=${'$'}am")
              MemoInt(a=am)
              MemoFloat(a=b)
              MemoDouble(a=c)
              MemoNotStable(a=d)
              MemoModel(a=e)
              MemoEnum(a=f)
              MemoStable(a=g)
              MemoMutableState(h)
              MemoState(i)
            }

            @Composable
            fun Main(v: ValueHolder, n: NotStable) {
              TestSkipping(
                a=1,
                b=1f,
                c=2.0,
                d=NotStable(),
                e=v,
                f=EnumState.One,
                g=StableClass(),
                h=mutableStateType,
                i=stateType
              )
            }
        """,
            """
            output = outerOutput
            val v = ValueHolder(0)
            Main(v, NotStable())
        """,
            {
                mapOf(
                    "outerOutput: ArrayList<String>" to output
                )
            }
        ).then {
            // Expect that all the methods are called in order
            assertEquals(
                "TestSkipping a=1 am=1, MemoInt a=1, MemoFloat, " +
                    "MemoDouble, MemoNotStable, MemoModelHolder, MemoEnum, MemoStable, " +
                    "MemoMutableState, MemoState",
                output.joinToString()
            )
            output.clear()
        }.then { activity ->
            // Expect TestSkipping and MemoNotStable to be called because the test forces an extra compose.
            assertEquals("TestSkipping a=1 am=1, MemoNotStable", output.joinToString())
            output.clear()

            // Change the model
            val button = activity.findViewById(101) as Button
            button.performClick()
        }.then {
            // Expect that only MemoInt (the parameter changed) and MemoNotStable (it has unstable parameters) were
            // called then expect a second compose which should only MemoNotStable
            assertEquals(
                "TestSkipping a=1 am=2, MemoInt a=2, MemoNotStable, " +
                    "TestSkipping a=1 am=2, MemoNotStable",
                output.joinToString()
            )
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testStableParameters_Lambdas(): Unit = ensureSetup {
        val output = ArrayList<String>()
        compose(
            """
            val m = mutableStateOf(0)

            var output = ArrayList<String>()
            val unchanged: () -> Unit = { }

            fun log(msg: String) { output.add(msg) }

            @Composable
            fun Container(content: @Composable () -> Unit) {
              log("Container")
              content()
            }

            @Composable
            fun NormalLambda(index: Int, lambda: () -> Unit) {
              log("NormalLambda(${'$'}index)")
              Button(text="text")
            }

            @Composable
            fun TestSkipping(unchanged: () -> Unit, changed: () -> Unit) {
              log("TestSkipping")
              Container {
                NormalLambda(index = 1, lambda = unchanged)
                NormalLambda(index = 2, lambda = unchanged)
                NormalLambda(index = 3, lambda = unchanged)
                NormalLambda(index = 4, lambda = changed)
              }
            }

            fun forceNewLambda(): () -> Unit {
                val capturedParameter = Math.random()
                return { capturedParameter }
            }

            @Composable
            fun Main(unchanged: () -> Unit) {
              Button(id=101, text="model ${'$'}{m.value}", onClick={ m.value++ })
              TestSkipping(unchanged = unchanged, changed = forceNewLambda())
            }
        """,
            """
            output = outerOutput
            Main(unchanged = unchanged)
        """,
            {
                mapOf(
                    "outerOutput: ArrayList<String>" to output
                )
            }
        ).then {
            // Expect that all the methods are called in order
            assertEquals(
                "TestSkipping, Container, NormalLambda(1), " +
                    "NormalLambda(2), NormalLambda(3), NormalLambda(4)",
                output.joinToString()
            )
            output.clear()
        }.then { activity ->
            // Expect nothing to occur with no changes
            assertEquals("", output.joinToString())
            output.clear()

            // Change the model
            val button = activity.findViewById(101) as Button
            button.performClick()
        }.then {
            // Expect only NormalLambda(4) to be called
            assertEquals(
                "TestSkipping, NormalLambda(4)",
                output.joinToString()
            )
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testRecomposeScope(): Unit = ensureSetup {
        compose(
            """
            val m = mutableStateOf(0)

            @Composable
            inline fun InlineContainer(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            fun Container(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            fun Leaf(v: Int) {}

            @Composable
            fun Inline() {
                InlineContainer {
                    Leaf(v = 1)
                }
            }

            @Composable
            fun Lambda() {
                val a = 1
                val b = 2
                Container {
                    TextView(text = "value = ${'$'}{m.value}", id = 100)
                    Leaf(v = 1)
                    Leaf(v = a)
                    Leaf(v = b)
                }
            }
            """,
            """
                Button(id=101, text="model ${'$'}{m.value}", onClick={ m.value++ })
                Lambda()
            """,
            noParameters
        ).then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "value = 0")
            val button = activity.findViewById<Button>(101)
            button.performClick()
        }.then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "value = 1")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testRecomposeScope_ReceiverScope(): Unit = ensureSetup {
        compose(
            """
            val m = mutableStateOf(0)

            class Receiver { var r: Int = 0 }

            @Composable
            fun Container(content: @Composable Receiver.() -> Unit) {
                Receiver().content()
            }

            @Composable
            fun Lambda() {
                Container {
                    TextView(text = "value = ${'$'}{m.value}", id = 100)
                }
            }
            """,
            """
                Button(id=101, text="model ${'$'}{m.value}", onClick={ m.value++ })
                Lambda()
            """,
            noParameters
        ).then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "value = 0")
            val button = activity.findViewById<Button>(101)
            button.performClick()
        }.then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "value = 1")
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testCompose_InlineReceiver(): Unit = ensureSetup {
        compose(
            """
            object Context {
                fun t() {}
            }

            @Composable
            inline fun b(content: @Composable Context.() -> Unit) { Context.content() }

            @Composable
            inline fun c(content: @Composable () -> Unit) { b { t(); content() } }
            """,
            "",
            noParameters
        ).then {
            // Nothing to do, tests code can be generated
        }
    }

    @Test
    @Ignore("b/173733968")
    fun testRecomposeScope_Method(): Unit = ensureSetup {
        compose(
            """
            val m = mutableStateOf(0)

            @Composable
            fun Leaf() { }

            class SelfCompose {
                var f1 = 0

                @Composable
                fun compose(f2: Int) {
                    TextView(
                      text = "f1=${'$'}f1, f2=${'$'}f2, m=${'$'}{m.value*f1*f2}",
                      id = 100
                    )
                }
            }

            @Composable
            fun InvokeSelfCompose() {
                val r = remember() { SelfCompose() }
                r.f1 = 1
                r.compose(f2 = 10)
                Leaf()
            }
            """,
            """
                Button(id=101, text="model ${'$'}{m.value}", onClick={ m.value++ })
                InvokeSelfCompose()
            """,
            noParameters
        ).then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "f1=1, f2=10, m=0")
            val button = activity.findViewById<Button>(101)
            button.performClick()
        }.then { activity ->
            assertEquals(activity.findViewById<TextView>(100).text, "f1=1, f2=10, m=10")
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun View.getComposedSet(tagId: Int): Set<String>? = getTag(tagId) as? Set<String>

private val noParameters = { emptyMap<String, String>() }

private inline fun <reified T : PsiElement> PsiElement.parentOfType(): T? = parentOfType(T::class)

private fun <T : PsiElement> PsiElement.parentOfType(vararg classes: KClass<out T>): T? {
    return PsiTreeUtil.getParentOfType(this, *classes.map { it.java }.toTypedArray())
}