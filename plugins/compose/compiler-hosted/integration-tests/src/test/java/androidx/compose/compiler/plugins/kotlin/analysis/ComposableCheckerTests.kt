package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import androidx.compose.compiler.plugins.kotlin.newConfiguration
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots

class ComposableCheckerTests : AbstractComposeDiagnosticsTest() {
    override fun setUp() {
        // intentionally don't call super.setUp() here since we are recreating an environment
        // every test
        System.setProperty(
            "user.dir",
            homeDir
        )
        System.setProperty(
            "idea.ignore.disabled.plugins",
            "true"
        )
    }

    private fun doTest(text: String, expectPass: Boolean) {
        val disposable = TestDisposable()
        val classPath = createClasspath()
        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.configureJdkClasspathRoots()

        val environment =
            KotlinCoreEnvironment.createForTests(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        setupEnvironment(environment)

        try {
            doTest(text, environment)
            if (!expectPass) {
                throw ExpectedFailureException(
                    "Test unexpectedly passed, but SHOULD FAIL"
                )
            }
        } catch (e: ExpectedFailureException) {
            throw e
        } catch (e: Exception) {
            if (expectPass) throw Exception(e)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    class ExpectedFailureException(message: String) : Exception(message)

    private fun check(expectedText: String) {
        doTest(expectedText, true)
    }

    private fun checkFail(expectedText: String) {
        doTest(expectedText, false)
    }

    fun testCfromNC() = check(
        """
        import androidx.compose.runtime.*

        @Composable fun C() {}
        fun <!COMPOSABLE_EXPECTED!>NC<!>() { <!COMPOSABLE_INVOCATION!>C<!>() }
    """
    )

    fun testNCfromC() = check(
        """
        import androidx.compose.runtime.*

        fun NC() {}
        @Composable fun C() { NC() }
    """
    )

    fun testCfromC() = check(
        """
        import androidx.compose.runtime.*

        @Composable fun C() {}
        @Composable fun C2() { C() }
    """
    )

    fun testCinCLambdaArg() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        @Composable fun C2(lambda: @Composable () -> Unit) { lambda() }
        @Composable fun C3() {
            C2 {
                C()
            }
        }
    """
    )

    fun testCinInlinedNCLambdaArg() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        inline fun InlineNC(lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            InlineNC {
                C()
            }
        }
    """
    )

    fun testCinNoinlineNCLambdaArg() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        <!NOTHING_TO_INLINE!>inline<!> fun NoinlineNC(noinline lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            NoinlineNC {
                <!COMPOSABLE_INVOCATION!>C<!>()
            }
        }
    """
    )

    fun testCinCrossinlineNCLambdaArg() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        inline fun CrossinlineNC(crossinline lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            CrossinlineNC {
                <!COMPOSABLE_INVOCATION!>C<!>()
            }
        }
    """
    )

    fun testCinNestedInlinedNCLambdaArg() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        inline fun InlineNC(lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            InlineNC {
                InlineNC {
                    C()
                }
            }
        }
    """
    )

    fun testCinLambdaArgOfNC() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        fun NC(lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            NC {
                <!COMPOSABLE_INVOCATION!>C<!>()
            }
        }
    """
    )

    fun testCinLambdaArgOfC() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C() { }
        @Composable fun C2(lambda: () -> Unit) { lambda() }
        @Composable fun C3() {
            C2 {
                <!COMPOSABLE_INVOCATION!>C<!>()
            }
        }
    """
    )

    fun testCinCPropGetter() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val cProp: Int @Composable get() = C()
    """
    )

    fun testCinNCPropGetter() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val <!COMPOSABLE_EXPECTED!>ncProp<!>: Int get() = <!COMPOSABLE_INVOCATION!>C<!>()
    """
    )

    fun testCinTopLevelInitializer() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val ncProp: Int = <!COMPOSABLE_INVOCATION!>C<!>()
    """
    )

    fun testCTypeAlias() = check(
        """
        import androidx.compose.runtime.*
        typealias Content = @Composable () -> Unit
        @Composable fun C() {}
        @Composable fun C2(content: Content) { content() }
        @Composable fun C3() {
            val inner: Content = { C() }
            C2 { C() }
            C2 { inner() }
        }
    """
    )

    fun testCfromComposableFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }
        @Composable fun B() { A { B() } }
    """
    )

    fun testCfromAnnotatedComposableFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }
        @Composable fun B() {
          val f = @Composable { B() }
          A(f)
        }
    """
    )

    fun testCfromComposableFunInterfaceArgument() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }

        @Composable fun B(a: (A) -> Unit) { a { B(a) } }
    """
    )

    fun testCfromComposableTypeAliasFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }
        typealias B = A

        @Composable fun C() { A { C() } }
    """
    )

    fun testCfromNonComposableFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { fun f() }
        @Composable fun B() {
          A {
            <!COMPOSABLE_INVOCATION!>B<!>()
          }
        }
    """
    )

    fun testCfromNonComposableFunInterfaceArgument() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { fun f() }

        @Composable fun B(a: (A) -> Unit) {
          a {
            <!COMPOSABLE_INVOCATION!>B<!>(a)
          }
        }
    """
    )

    fun testPreventedCaptureOnInlineLambda() = check(
        """
        import androidx.compose.runtime.*

        @Composable inline fun A(
            lambda: @DisallowComposableCalls () -> Unit
        ) { if (Math.random() > 0.5) lambda() }
        @Composable fun B() {}

        @Composable fun C() {
            A { <!CAPTURED_COMPOSABLE_INVOCATION!>B<!>() }
        }
    """
    )

    fun testComposableReporting001() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            fun myStatelessFunctionalComponent() {
                Leaf()
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun myStatelessFunctionalComponent() {
                Leaf()
            }

            @Composable
            fun foo() {
                myStatelessFunctionalComponent()
            }
        """
        )
    }

    fun testComposableReporting002() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            val myLambda1 = { Leaf() }
            val myLambda2: () -> Unit = { Leaf() }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            val myLambda1 = @Composable { Leaf() }
            val myLambda2: @Composable ()->Unit = { Leaf() }
        """
        )
    }

    fun testComposableReporting006() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            fun foo() {
                val bar = {
                    Leaf()
                }
                bar()
                System.out.println(bar)
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                val bar = @Composable {
                    Leaf()
                }
                bar()
                System.out.println(bar)
            }
        """
        )
    }

    fun testComposableReporting007() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            fun foo(content: @Composable ()->Unit) {
                <!SVC_INVOCATION!>content<!>()
            }
        """
        )
    }

    fun testComposableReporting008() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable fun Leaf() {}

            fun foo() {
                val bar: @Composable ()->Unit = @Composable {
                    Leaf()
                }
                <!COMPOSABLE_INVOCATION!>bar<!>()
                System.out.println(bar)
            }
        """
        )
    }

    fun testComposableReporting009() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Leaf() {}

            @Composable
            fun myStatelessFunctionalComponent() {
                Leaf()
            }

            fun <!COMPOSABLE_EXPECTED!>noise<!>() {
                <!COMPOSABLE_INVOCATION!>myStatelessFunctionalComponent<!>()
            }
        """
        )
    }

    fun testComposableReporting017() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable fun Leaf() {}

            @Composable
            fun Foo(content: ()->Unit) {
                content()
            }

            @Composable
            fun test() {
                Foo { Leaf() }
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Leaf() {}

            @Composable
            fun Foo(content: ()->Unit) {
                content()
            }

            @Composable
            fun test() {
                Foo { <!COMPOSABLE_INVOCATION!>Leaf<!>() }
            }
        """
        )
    }

    fun testComposableReporting018() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            fun foo() {
                val myVariable: ()->Unit = @Composable { Leaf() }
                System.out.println(myVariable)
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            fun foo() {
                val myVariable: ()->Unit = <!TYPE_MISMATCH!>@Composable { Leaf() }<!>
                System.out.println(myVariable)
            }
        """
        )
    }

    fun testComposableReporting021() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach @Composable { value: Int ->
                    Leaf()
                    System.out.println(value)
                }
            }
        """
        )
    }

    fun testComposableReporting022() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            fun <!COMPOSABLE_EXPECTED!>foo<!>() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach { value: Int ->
                    <!COMPOSABLE_INVOCATION!>Leaf<!>()
                    println(value)
                }
            }
        """
        )
    }

    fun testComposableReporting023() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach { value: Int ->
                    Leaf()
                    println(value)
                }
            }
        """
        )
    }

    fun testComposableReporting024() {
        check(
            """
            import androidx.compose.runtime.*

            var x: (@Composable () -> Unit)? = null

            class Foo
            fun Foo.setContent(content: @Composable () -> Unit) {
                x = content
            }

            @Composable
            fun Leaf() {}

            fun Example(foo: Foo) {
                foo.setContent { Leaf() }
            }
        """
        )
    }

    fun testComposableReporting024x() {
        check(
            """
            import androidx.compose.runtime.*

            var x: (@Composable () -> Unit)? = null

            fun <!COMPOSABLE_EXPECTED!>Example<!>(content: @Composable () -> Unit) {
                x = content
                <!COMPOSABLE_INVOCATION!>content<!>()
            }
        """
        )
    }

    fun testComposableReporting025() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                listOf(1,2,3,4,5).forEach { Leaf() }
            }
        """
        )
    }

    fun testComposableReporting026() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun Group(content: @Composable () -> Unit) { content() }

            @Composable
            fun foo() {
                Group {
                    Leaf()
                }
            }
        """
        )
    }

    fun testComposableReporting027() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun Leaf() {}

            @Composable
            fun Group(content: @Composable () -> Unit) { content() }

            @Composable
            fun foo() {
                Group {
                    listOf(1,2,3).forEach {
                        Leaf()
                    }
                }
            }
        """
        )
    }

    fun testComposableReporting028() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            fun foo(v: @Composable ()->Unit) {
                val myVariable: ()->Unit = v
                myVariable()
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            fun foo(v: @Composable ()->Unit) {
                val myVariable: ()->Unit = <!TYPE_MISMATCH!>v<!>
                myVariable()
            }
        """
        )
    }

    fun testComposableReporting030() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun foo() {
                val myVariable: @Composable ()->Unit = {};
                myVariable()
            }
        """
        )
    }

    fun testComposableReporting032() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun MyComposable(content: @Composable ()->Unit) { content() }

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                MyComposable { Leaf() }
            }
        """
        )
    }

    fun testComposableReporting033() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun MyComposable(content: @Composable ()->Unit) { content() }

            @Composable
            fun Leaf() {}

            @Composable
            fun foo() {
                MyComposable(content={ Leaf() })
            }
        """
        )
    }

    fun testComposableReporting034() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            fun identity(f: ()->Unit): ()->Unit { return f; }

            @Composable
            fun test(f: @Composable ()->Unit) {
                val f2: @Composable ()->Unit = identity(f);
                f2()
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            fun identity(f: ()->Unit): ()->Unit { return f; }

            @Composable
            fun test(f: @Composable ()->Unit) {
                val f2: @Composable ()->Unit = <!TYPE_MISMATCH!>identity (<!TYPE_MISMATCH!>f<!>)<!>;
                f2()
            }
        """
        )
    }

    fun testComposableReporting035() {
        check(
            """
            import androidx.compose.runtime.*

            @Composable
            fun Foo(x: String) {
                @Composable operator fun String.invoke() {}
                x()
            }
        """
        )
    }

    fun testComposableReporting039() {
        check(
            """
            import androidx.compose.runtime.*

            fun composeInto(l: @Composable ()->Unit) { System.out.println(l) }

            fun Foo() {
                composeInto {
                    Baz()
                }
            }

            fun Bar() {
                Foo()
            }
            @Composable fun Baz() {}
        """
        )
    }

    fun testComposableReporting041() {
        check(
            """
            import androidx.compose.runtime.*

            typealias COMPOSABLE_UNIT_LAMBDA = @Composable () -> Unit

            @Composable
            fun ComposeWrapperComposable(content: COMPOSABLE_UNIT_LAMBDA) {
                MyComposeWrapper {
                    content()
                }
            }

            @Composable fun MyComposeWrapper(content: COMPOSABLE_UNIT_LAMBDA) {
                print(content.hashCode())
            }
        """
        )
    }

    fun testComposableReporting043() {
        check(
            """
            import androidx.compose.runtime.*

            @Composable
            fun FancyButton() {}

            fun <!COMPOSABLE_EXPECTED!>Noise<!>() {
                <!COMPOSABLE_INVOCATION!>FancyButton<!>()
            }
        """
        )
    }

    fun testComposableReporting044() {
        check(
            """
            import androidx.compose.runtime.*

            typealias UNIT_LAMBDA = () -> Unit

            @Composable
            fun FancyButton() {}

            @Composable
            fun Noise() {
                FancyButton()
            }
        """
        )
    }

    fun testComposableReporting045() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable
            fun foo() {
                val bar = @Composable {}
                bar()
                System.out.println(bar)
            }
        """
        )
    }

    fun testComposableReporting048() {
        // Type inference for non-null @Composable lambdas
        checkFail(
            """
            import androidx.compose.runtime.*

            val lambda: @Composable (() -> Unit)? = null

            @Composable
            fun Foo() {
	        // Should fail as null cannot be coerced to non-null
                Bar(lambda)
                Bar(null)
                Bar {}
            }

            @Composable
            fun Bar(child: @Composable () -> Unit) {
                child()
            }
        """
        )

        // Type inference for nullable @Composable lambdas, with no default value
        check(
            """
            import androidx.compose.runtime.*

            val lambda: @Composable (() -> Unit)? = null

            @Composable
            fun Foo() {
                Bar(lambda)
                Bar(null)
                Bar {}
            }

            @Composable
            fun Bar(child: @Composable (() -> Unit)?) {
                child?.invoke()
            }
        """
        )

        // Type inference for nullable @Composable lambdas, with a nullable default value
        check(
            """
            import androidx.compose.runtime.*

            val lambda: @Composable (() -> Unit)? = null

            @Composable
            fun Foo() {
                Bar()
                Bar(lambda)
                Bar(null)
                Bar {}
            }

            @Composable
            fun Bar(child: @Composable (() -> Unit)? = null) {
                child?.invoke()
            }
        """
        )

        // Type inference for nullable @Composable lambdas, with a non-null default value
        check(
            """
            import androidx.compose.runtime.*

            val lambda: @Composable (() -> Unit)? = null

            @Composable
            fun Foo() {
                Bar()
                Bar(lambda)
                Bar(null)
                Bar {}
            }

            @Composable
            fun Bar(child: @Composable (() -> Unit)? = {}) {
                child?.invoke()
            }
        """
        )
    }

    fun testComposableReporting049() {
        check(
            """
            import androidx.compose.runtime.*
            fun foo(<!WRONG_ANNOTATION_TARGET!>@Composable<!> bar: ()->Unit) {
                println(bar)
            }
        """
        )
    }

    fun testComposableReporting050() {
        check(
            """
            import androidx.compose.runtime.*;

            val foo: Int @Composable get() = 123

            fun <!COMPOSABLE_EXPECTED!>App<!>() {
                <!COMPOSABLE_INVOCATION!>foo<!>
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            val foo: Int @Composable  get() = 123

            @Composable
            fun App() {
                println(foo)
            }
        """
        )
    }

    fun testComposableReporting051() {
        checkFail(
            """
            import androidx.compose.runtime.*;

            class A {
                @Composable val bar get() = 123
            }

            @Composable val A.bam get() = 123

            fun App() {
                val a = A()
                a.bar
            }
        """
        )
        checkFail(
            """
            import androidx.compose.runtime.*;

            class A {
                @Composable val bar get() = 123
            }

            @Composable val A.bam get() = 123

            fun App() {
                val a = A()
                a.bam
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.*;

            class A {
                val bar @Composable get() = 123
            }

            val A.bam @Composable get() = 123

            @Composable
            fun App() {
                val a = A()
                a.bar
                a.bam
                with(a) {
                    bar
                    bam
                }
            }
        """
        )
    }

    fun testComposableReporting052() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Foo() {}

            val <!COMPOSABLE_EXPECTED!>bam<!>: Int get() {
                <!COMPOSABLE_INVOCATION!>Foo<!>()
                return 123
            }
        """
        )

        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Foo() {}

            val bam: Int @Composable get() {
                Foo()
                return 123
            }
        """
        )
    }

    fun testComposableReporting053() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun foo(): Int = 123

            fun <!COMPOSABLE_EXPECTED!>App<!>() {
                val x = <!COMPOSABLE_INVOCATION!>foo<!>()
                print(x)
            }
        """
        )
    }

    fun testComposableReporting054() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Foo() {}

            val <!COMPOSABLE_EXPECTED!>y<!>: Any get() =
            <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(1) }

            fun App() {
                val x = object {
                  val <!COMPOSABLE_EXPECTED!>a<!> get() =
                  <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(2) }
                  val c @Composable get() = remember { mutableStateOf(4) }
                  @Composable fun bar() { Foo() }
                  fun <!COMPOSABLE_EXPECTED!>foo<!>() {
                    <!COMPOSABLE_INVOCATION!>Foo<!>()
                  }
                }
                class Bar {
                  val <!COMPOSABLE_EXPECTED!>b<!> get() =
                  <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(6) }
                  val c @Composable get() = remember { mutableStateOf(7) }
                }
                fun <!COMPOSABLE_EXPECTED!>Bam<!>() {
                    <!COMPOSABLE_INVOCATION!>Foo<!>()
                }
                @Composable fun Boo() {
                    Foo()
                }
                print(x)
            }
        """
        )
    }

    fun testComposableReporting055() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun Foo() {}

            @Composable fun App() {
                val x = object {
                  val <!COMPOSABLE_EXPECTED!>a<!> get() = <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(2) }
                  val c @Composable get() = remember { mutableStateOf(4) }
                  fun <!COMPOSABLE_EXPECTED!>foo<!>() {
                    <!COMPOSABLE_INVOCATION!>Foo<!>()
                  }
                  @Composable fun bar() { Foo() }
                }
                class Bar {
                  val <!COMPOSABLE_EXPECTED!>b<!> get() = <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(6) }
                  val c @Composable get() = remember { mutableStateOf(7) }
                }
                fun <!COMPOSABLE_EXPECTED!>Bam<!>() {
                    <!COMPOSABLE_INVOCATION!>Foo<!>()
                }
                @Composable fun Boo() {
                    Foo()
                }
                print(x)
            }
        """
        )
    }

    fun testComposableReporting057() {
        check(
            """
            import androidx.compose.runtime.*;

            @Composable fun App() {
                val x = object {
                  val b = remember { mutableStateOf(3) }
                }
                class Bar {
                  val a = <!COMPOSABLE_INVOCATION!>remember<!> { mutableStateOf(5) }
                }
                print(x)
            }
        """
        )
    }

    fun testDisallowComposableCallPropagation() = check(
        """
        import androidx.compose.runtime.*
        class Foo
        @Composable inline fun a(block1: @DisallowComposableCalls () -> Foo): Foo {
            return block1()
        }
        @Composable inline fun b(<!MISSING_DISALLOW_COMPOSABLE_CALLS_ANNOTATION!>block2: () -> Foo<!>): Foo {
          return a { block2() }
        }
        @Composable inline fun c(block2: @DisallowComposableCalls () -> Foo): Foo {
          return a { block2() }
        }
    """
    )

    fun testComposableLambdaToAll() = check(
        """
        import androidx.compose.runtime.*

        fun foo() {
            val lambda = @Composable { }
            println(lambda)  // println accepts Any, verify no type mismatach.
        }
    """
    )

    fun testReadOnlyComposablePropagation() = check(
        """
        import androidx.compose.runtime.*

        @Composable @ReadOnlyComposable
        fun readOnly(): Int = 10
        val readonlyVal: Int
            @Composable @ReadOnlyComposable get() = 10

        @Composable
        fun normal(): Int = 10
        val normalVal: Int
            @Composable get() = 10

        @Composable
        fun test1() {
            print(readOnly())
            print(readonlyVal)
        }

        @Composable @ReadOnlyComposable
        fun test2() {
            print(readOnly())
            print(readonlyVal)
        }

        @Composable
        fun test3() {
            print(readOnly())
            print(normal())
            print(readonlyVal)
            print(normalVal)
        }

        @Composable @ReadOnlyComposable
        fun test4() {
            print(readOnly())
            print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normal<!>())
            print(readonlyVal)
            print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normalVal<!>)
        }

        val test5: Int
            @Composable
            get() {
                print(readOnly())
                print(readonlyVal)
                return 10
            }

        val test6: Int
            @Composable @ReadOnlyComposable
            get() {
                print(readOnly())
                print(readonlyVal)
                return 10
            }

        val test7: Int
            @Composable
            get() {
                print(readOnly())
                print(normal())
                print(readonlyVal)
                print(normalVal)
                return 10
            }

        val test8: Int
            @Composable @ReadOnlyComposable
            get() {
                print(readOnly())
                print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normal<!>())
                print(readonlyVal)
                print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normalVal<!>)
                return 10
            }
    """
    )
}
