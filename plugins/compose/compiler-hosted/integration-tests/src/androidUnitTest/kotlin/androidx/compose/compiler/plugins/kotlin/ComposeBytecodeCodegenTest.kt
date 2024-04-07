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

import kotlin.test.assertFalse
import org.junit.Assume.assumeFalse
import org.junit.Test

class ComposeBytecodeCodegenTest(useFir: Boolean) : AbstractCodegenTest(useFir) {
//    b/179279455
//    @Test
//    fun testObserveLowering() {
//        testCompileWithViewStubs(
//            """
//            import androidx.compose.runtime.MutableState
//            import androidx.compose.runtime.mutableStateOf
//
//            @Composable
//            fun SimpleComposable() {
//                FancyButton(state=mutableStateOf(0))
//            }
//
//            @Composable
//            fun FancyButton(state: MutableState<Int>) {
//               Button(
//                 text=("Clicked "+state.value+" times"),
//                 onClick={state.value++},
//                 id=42
//               )
//            }
//        """
//        )
//    }

    @Test
    fun testEmptyComposeFunction() {
        testCompile(
            """
        import androidx.compose.runtime.*

        class Foo {
            @Composable
            operator fun invoke() {}
        }
        """
        )
    }

//    "b/179279455"
//    @Test
//    fun testSingleViewCompose() {
//        testCompileWithViewStubs(
//            """
//        class Foo {
//            @Composable
//            operator fun invoke() {
//                TextView()
//            }
//        }
//        """
//        )
//    }

//    "b/179279455"
//    @Test
//    fun testMultipleRootViewCompose() {
//        testCompileWithViewStubs(
//            """
//        class Foo {
//            @Composable
//            operator fun invoke() {
//                TextView()
//                TextView()
//                TextView()
//            }
//        }
//        """
//        )
//    }

//    "b/179279455"
//    @Test
//    fun testNestedViewCompose() {
//        testCompileWithViewStubs(
//            """
//        class Foo {
//            @Composable
//            operator fun invoke() {
//                LinearLayout {
//                    TextView()
//                    LinearLayout {
//                        TextView()
//                        TextView()
//                    }
//                }
//            }
//        }
//        """
//        )
//    }

    @Test
    fun testSingleComposite() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                Bar()
            }
        }
        """
        )
    }

    @Test
    fun testMultipleRootComposite() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                Bar()
                Bar()
                Bar()
            }
        }
        """
        )
    }

//    "b/179279455"
//    @Test
//    fun testViewAndComposites() {
//        testCompileWithViewStubs(
//            """
//        @Composable
//        fun Bar() {}
//
//        class Foo {
//            @Composable
//            operator fun invoke() {
//                LinearLayout {
//                    Bar()
//                }
//            }
//        }
//        """
//        )
//    }

    @Test
    fun testForEach() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                listOf(1, 2, 3).forEach {
                    Bar()
                }
            }
        }
        """
        )
    }

    @Test
    fun testForLoop() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        class Foo {
            @Composable
            operator fun invoke() {
                for (i in listOf(1, 2, 3)) {
                    Bar()
                }
            }
        }
        """
        )
    }

    @Test
    fun testEarlyReturns() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) return
                else "" // TODO: Remove this line when fixed upstream
                Bar()
            }
        }
        """
        )
    }

    @Test
    fun testConditionalRendering() {
        testCompile(
            """
         import androidx.compose.runtime.*

        @Composable
        fun Bar() {}

        @Composable
        fun Bam() {}

        class Foo {
            var visible: Boolean = false
            @Composable
            operator fun invoke() {
                if (!visible) {
                    Bar()
                } else {
                    Bam()
                }
            }
        }
        """
        )
    }

//    "b/179279455"
//    @Test
//    fun testChildrenWithTypedParameters() {
//        testCompileWithViewStubs(
//            """
//        @Composable fun HelperComponent(
//            content: @Composable (title: String, rating: Int) -> Unit
//        ) {
//            content("Hello World!", 5)
//            content("Kompose is awesome!", 5)
//            content("Bitcoin!", 4)
//        }
//
//        class MainComponent {
//            var name = "World"
//            @Composable
//            operator fun invoke() {
//                HelperComponent { title: String, rating: Int ->
//                    TextView(text=(title+" ("+rating+" stars)"))
//                }
//            }
//        }
//        """
//        )
//    }

//    "b/179279455"
//    @Test
//    fun testChildrenCaptureVariables() {
//        testCompileWithViewStubs(
//            """
//        @Composable fun HelperComponent(content: @Composable () -> Unit) {
//        }
//
//        class MainComponent {
//            var name = "World"
//            @Composable
//            operator fun invoke() {
//                val childText = "Hello World!"
//                HelperComponent {
//                    TextView(text=childText + name)
//                }
//            }
//        }
//        """
//        )
//    }

    @Test
    fun testChildrenDeepCaptureVariables() {
        testCompile(
            """
        import android.widget.*
        import androidx.compose.runtime.*

        @Composable fun A(content: @Composable () -> Unit) {
            content()
        }

        @Composable fun B(content: @Composable () -> Unit) {
            content()
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                A {
                    B {
                        println(childText + name)
                    }
                }
            }
        }
        """
        )
    }

    @Test
    fun testChildrenDeepCaptureVariablesWithParameters() {
        testCompile(
            """
        import android.widget.*
        import androidx.compose.runtime.*

        @Composable fun A(content: @Composable (x: String) -> Unit) {
            content("")
        }

        @Composable fun B(content: @Composable (y: String) -> Unit) {
            content("")
        }

        class MainComponent {
            var name = "World"
            @Composable
            operator fun invoke() {
                val childText = "Hello World!"
                A { x ->
                    B { y ->
                        println(childText + name + x + y)
                    }
                }
            }
        }
        """
        )
    }

//    "b/179279455"
//    @Test
//    fun testChildrenOfNativeView() {
//        testCompileWithViewStubs(
//            """
//        class MainComponent {
//            @Composable
//            operator fun invoke() {
//                LinearLayout {
//                    TextView(text="some child content2!")
//                    TextView(text="some child content!3")
//                }
//            }
//        }
//        """
//        )
//    }

//    "b/179279455"
//    @Test
//    fun testIrSpecial() {
//        testCompileWithViewStubs(
//            """
//        @Composable fun HelperComponent(content: @Composable () -> Unit) {}
//
//        class MainComponent {
//            @Composable
//            operator fun invoke() {
//                val x = "Hello"
//                val y = "World"
//                HelperComponent {
//                    for(i in 1..100) {
//                        TextView(text=x+y+i)
//                    }
//                }
//            }
//        }
//        """
//        )
//    }

    @Test
    fun testGenericsInnerClass() {
        testCompile(
            """
        import androidx.compose.runtime.*

        class A<T>(val value: T) {
            @Composable fun Getter(x: T? = null) {
            }
        }

        @Composable
        fun doStuff() {
            val a = A(123)

            // a.Getter() here has a bound type argument through A
            a.Getter(x=456)
        }
        """
        )
    }

    @Test
    fun testXGenericConstructorParams() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable fun <T> A(
            value: T,
            list: List<T>? = null
        ) {

        }

        @Composable
        fun doStuff() {
            val x = 123

            // we can create element with just value, no list
            A(value=x)

            // if we add a list, it can infer the type
            A(
                value=x,
                list=listOf(234, x)
            )
        }
        """
        )
    }

    @Test
    fun testSimpleNoArgsComponent() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable
        fun Simple() {}

        @Composable
        fun run() {
            Simple()
        }
        """
        )
    }

    @Test
    fun testDotQualifiedObjectToClass() {
        testCompile(
            """
        import androidx.compose.runtime.*

        object Obj {
            @Composable
            fun B() {}
        }

        @Composable
        fun run() {
            Obj.B()
        }
        """
        )
    }

    @Test
    fun testLocalLambda() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable
        fun Simple() {}

        @Composable
        fun run() {
            val foo = @Composable { Simple() }
            foo()
        }
        """
        )
    }

    @Test
    fun testPropertyLambda() {
        testCompile(
            """
        import androidx.compose.runtime.*

        class Test(var content: @Composable () () -> Unit) {
            @Composable
            operator fun invoke() {
                content()
            }
        }
        """
        )
    }

    @Test
    fun testLambdaWithArgs() {
        // FIR does not support named lambda arguments
        // We will deprecate this in Compose, see b/281677454
        assumeFalse(useFir)
        testCompile(
            """
        import androidx.compose.runtime.*

        class Test(var content: @Composable (x: Int) -> Unit) {
            @Composable
            operator fun invoke() {
                content(x=123)
            }
        }
        """
        )
    }

    @Test
    fun testLocalMethod() {
        testCompile(
            """
        import androidx.compose.runtime.*

        class Test {
            @Composable
            fun doStuff() {}
            @Composable
            operator fun invoke() {
                doStuff()
            }
        }
        """
        )
    }

    @Test
    fun testSimpleLambdaChildren() {
        testCompile(
            """
        import androidx.compose.runtime.*
        import android.widget.*
        import android.content.*

        @Composable fun Example(content: @Composable () -> Unit) {

        }

        @Composable
        fun run(text: String) {
            Example {
                println("hello ${"$"}text")
            }
        }
        """
        )
    }

    @Test
    fun testFunctionComponentsWithChildrenSimple() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable
        fun Example(content: @Composable () -> Unit) {}

        @Composable
        fun run(text: String) {
            Example {
                println("hello ${"$"}text")
            }
        }
        """
        )
    }

    @Test
    fun testFunctionComponentWithChildrenOneArg() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable
        fun Example(content: @Composable (String) -> Unit) {}

        @Composable
        fun run(text: String) {
            Example { x ->
                println("hello ${"$"}x")
            }
        }
        """
        )
    }

    @Test
    fun testKtxLambdaInForLoop() {
        testCompile(
            """
        import androidx.compose.runtime.*
        import android.widget.TextView

        @Composable
        fun foo() {
            val lambda = @Composable {  }
            for(x in 1..5) {
                lambda()
                lambda()
            }
        }
        """
        )
    }

//    "b/179279455"
//    @Test
//    fun testKtxLambdaInIfElse() {
//        testCompileWithViewStubs(
//            """
//        @Composable
//        fun foo(x: Boolean) {
//            val lambda = @Composable { TextView(text="Hello World") }
//            if(true) {
//                lambda()
//                lambda()
//                lambda()
//            } else {
//                lambda()
//            }
//        }
//        """
//        )
//    }

    @Test
    fun testKtxVariableTagsProperlyCapturedAcrossKtxLambdas() {
        testCompile(
            """
        import androidx.compose.runtime.*

        @Composable fun Foo(content: @Composable (sub: @Composable () -> Unit) -> Unit) {

        }

        @Composable fun Boo(content: @Composable () -> Unit) {

        }

        class Bar {
            @Composable
            operator fun invoke() {
                Foo { sub ->
                    Boo {
                        sub()
                    }
                }
            }
        }
        """
        )
    }

    @Test
    fun testInvocableObject() {
        testCompile(
            """
        import androidx.compose.runtime.*

        class Foo { }
        @Composable
        operator fun Foo.invoke() {  }

        @Composable
        fun test() {
            val foo = Foo()
            foo()
        }
        """
        )
    }

    @Test
    fun testRecursiveLocalFunction() = validateBytecode(
        """
            import androidx.compose.runtime.*

            @Composable fun Surface(content: @Composable () -> Unit) {}

            @Composable
            fun MyComposable(){
                @Composable
                fun LocalComposable(){
                    Surface { LocalComposable() }
                }
            }
        """,
        validate = {
            assertFalse(
                it.contains("ComposableSingletons"),
                message = "ComposableSingletons class should not be generated"
            )
        }
    )

    // regression test for https://youtrack.jetbrains.com/issue/KT-65791
    @Test
    fun testCrossinlineCapture() = testCompile(
        """
            import androidx.compose.runtime.*

            @Composable
            fun LazyColumn(
                content: () -> Unit
            ): Unit = TODO()

            @Composable
            inline fun Box(content: @Composable () -> Unit) {
                content()
            }

            @Composable
            inline fun ItemsPage(
                crossinline itemContent: @Composable (Int) -> Unit,
            ) {
                Box {
                    LazyColumn {
                        val lambda: @Composable (item: Int) -> Unit = {
                            itemContent(it)
                        }
                    }
                }
            }

            @Composable
            fun SearchResultScreen() {
                ItemsPage(
                    itemContent = {},
                )
            }
        """
    )
}
