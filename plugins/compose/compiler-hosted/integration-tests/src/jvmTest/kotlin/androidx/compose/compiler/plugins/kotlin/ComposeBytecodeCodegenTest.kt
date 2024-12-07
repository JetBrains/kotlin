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

import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/* ktlint-disable max-line-length */
class ComposeBytecodeCodegenTest(useFir: Boolean) : AbstractCodegenTest(useFir) {

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

    @Test
    fun testChildrenDeepCaptureVariables() {
        testCompile(
            """
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

    @Test
    fun composeValueClassDefaultParameter() =
        validateBytecode(
            """
                import androidx.compose.runtime.*

                @JvmInline
                value class Data(val string: String)
                @JvmInline
                value class IntData(val value: Int)

                @Composable fun Example(data: Data = Data(""), intData: IntData = IntData(0)) {}
            """,
            validate = {
                // select Example function body
                val func = Regex("public final static Example[\\s\\S]*?LOCALVARIABLE")
                    .findAll(it)
                    .single()
                assertFalse(message = "Function body should not contain a not-null check.") {
                    func.value.contains("Intrinsics.checkNotNullParameter")
                }
                val stub = Regex("public final static synthetic Example[\\s\\S]*?LOCALVARIABLE")
                    .findAll(it)
                    .single()
                assertTrue(message = "Function stub should contain a not-null check.") {
                    stub.value.contains("Intrinsics.checkNotNullParameter")
                }
            },
        )

    @Test // regression test for 336571300
    fun test_groupAroundIfComposeCallInIfConditionWithShortCircuit() = testCompile(
        source = """
            import androidx.compose.runtime.*

            @Composable
            fun Test() {
                ReceiveValue(if (state && getCondition()) 0 else 1)
            }

            val state by mutableStateOf(true)

            @Composable
            fun getCondition() = remember { mutableStateOf(false) }.value

            @Composable
            fun ReceiveValue(value: Int) { }
        """
    )

    @Test
    fun testDefaultParametersInAbstractFunctions() = validateBytecode(
        """
            import androidx.compose.runtime.*

            interface Test {
                @Composable fun foo(param: Int = remember { 0 })
            }

            class TestImpl : Test {
                @Composable override fun foo(param: Int) {}
            }

            @Composable fun CallWithDefaults(test: Test) {
                test.foo()
                test.foo(0)
            }
        """,
        validate = {
            assertTrue(
                it.contains(
                    "INVOKESTATIC test/Test%ComposeDefaultImpls.foo%default (ILtest/Test;Landroidx/compose/runtime/Composer;II)V"
                ),
                "default static functions should be generated in ComposeDefaultsImpl class"
            )
        }
    )

    @Ignore("b/357878245")
    @Test
    fun testDefaultParametersInOpenFunctions() = validateBytecode(
        """
            import androidx.compose.runtime.*

            interface Test {
                @Composable fun bar(param: Int = remember { 0 }): Int = param
            }

            class TestImpl : Test {
                @Composable override fun bar(param: Int): Int {
                    return super.bar(param)
                }
            }

            @Composable fun CallWithDefaults(test: Test) {
                test.bar()
                test.bar(0)
            }
        """,
        validate = {
            assertTrue(
                it.contains(
                    "INVOKESTATIC test/Test%ComposeDefaultImpls.foo%default (ILtest/Test;Landroidx/compose/runtime/Composer;II)V"
                ),
                "default static functions should be generated in ComposeDefaultsImpl class"
            )
        }
    )

    @Test
    fun testMemoizingFromDelegate() = testCompile(
        """
            import androidx.compose.runtime.*

            class ClassWithData(
                val action: Int = 0,
            )

            fun getData(): ClassWithData = TODO()

            @Composable
            fun StrongSkippingIssue(
                data: ClassWithData
            ) {
                val state by remember { mutableStateOf("") }
                val action by data::action
                val action1 by getData()::action
                { 
                    action
                }
                {
                    action1
                }
                {
                    state
                }
            }
        """
    )

    @Test
    fun inlineClassWithComposableLambda() {
        testCompile(
            """
                import androidx.compose.runtime.*
                import kotlin.jvm.JvmInline
                
                @JvmInline
                value class ComposableContent(val content: @Composable () -> Unit)
            """
        )
    }

    // regression test for b/339322843
    @Test
    fun testPropertyReferenceInDelegate() {
        testCompile(
            """
                import androidx.compose.runtime.*
                import kotlin.reflect.KProperty

                object MaterialTheme {
                    val background: Int = 0
                }
                
                fun interface ThemeToken<T> {

                    @Composable
                    @ReadOnlyComposable
                    fun MaterialTheme.resolve(): T
                
                    @Composable
                    @ReadOnlyComposable
                    operator fun getValue(thisRef: Any?, property: KProperty<*>) = MaterialTheme.resolve()
                }
                
                @get:Composable
                val background by ThemeToken { background }
            """
        )
    }

    @Test
    fun testNoRepeatingLineNumbersInLambda() {
        validateBytecode(
            """
                import androidx.compose.runtime.*

                @Composable fun App() {}

                class Activity {
                    fun setContent(content: @Composable () -> Unit) {}
                    
                    fun onCreate() {
                        setContent {
                            println()
                            App()
                        }
                    }
                }
            """,
            validate = { bytecode ->
                val classesRegex = Regex("final class (.*?) \\{[\\S\\s]*?^}", RegexOption.MULTILINE)
                val matches = classesRegex.findAll(bytecode)
                val lambdaClass = matches
                    .single { it.groups[1]?.value?.startsWith("test/ComposableSingletons%TestKt%lambda%") == true }
                    .value
                val invokeRegex = Regex("public final invoke([\\s\\S]*?)LOCALVARIABLE")
                val invokeMethod = invokeRegex.find(lambdaClass)?.value ?: error("Could not find invoke method in $lambdaClass")
                val lineNumbers = invokeMethod.lines()
                    .mapNotNull {
                        it.takeIf { it.contains("LINENUMBER") }
                    }
                    .joinToString("\n")

                assertEquals(
                    """
                    LINENUMBER 19 L3
                    LINENUMBER 20 L5
                    LINENUMBER 21 L6
                    """.trimIndent(),
                    lineNumbers.trimIndent()
                )
            }
        )
    }

    // regression test for b/376148043
    @Test
    fun testUpdatingLambdaText() {
        val oldBytecode = compileBytecode(
            """
                  import androidx.compose.runtime.*

                  @Composable fun composableFun3() {
                    val a = { }
                  }
                  @Composable fun composableFun4() {
                    val a = { } 
                  }
            """,
            className = "TestClass",
        )

        val newBytecode = compileBytecode(
            """
                  import androidx.compose.runtime.*

                  @Composable fun composableFun3() {
                    val a = { "hello" }
                  }
                  @Composable fun composableFun4() {
                    val a = { } 
                  }
            """,
            className = "TestClass",
        )

        val function4Regex = Regex("composableFun4[\\s\\S]*?LOCALVARIABLE")
        val function4 = function4Regex.find(newBytecode)?.value ?: error("Could not find function4 in new bytecode")
        val oldFunction4 = function4Regex.find(oldBytecode)?.value ?: error("Could not find function4 in old bytecide")
        assertEquals(oldFunction4, function4)
    }
}
