package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import org.junit.Test

class ComposableCheckerTests : AbstractComposeDiagnosticsTest() {
    @Test
    fun testCfromNC() = check(
        """
        import androidx.compose.runtime.*

        @Composable fun C() {}
        fun <!COMPOSABLE_EXPECTED!>NC<!>() { <!COMPOSABLE_INVOCATION!>C<!>() }
    """
    )

    @Test
    fun testNCfromC() = check(
        """
        import androidx.compose.runtime.*

        fun NC() {}
        @Composable fun C() { NC() }
    """
    )

    @Test
    fun testCfromC() = check(
        """
        import androidx.compose.runtime.*

        @Composable fun C() {}
        @Composable fun C2() { C() }
    """
    )

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    fun testCinCPropGetter() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val cProp: Int @Composable get() = C()
    """
    )

    @Test
    fun testCinNCPropGetter() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val <!COMPOSABLE_EXPECTED!>ncProp<!>: Int get() = <!COMPOSABLE_INVOCATION!>C<!>()
    """
    )

    @Test
    fun testCinTopLevelInitializer() = check(
        """
        import androidx.compose.runtime.*
        @Composable fun C(): Int { return 123 }
        val ncProp: Int = <!COMPOSABLE_INVOCATION!>C<!>()
    """
    )

    @Test
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

    @Test
    fun testCfromComposableFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }
        @Composable fun B() { A { B() } }
    """
    )

    @Test
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

    @Test
    fun testCfromComposableFunInterfaceArgument() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }

        @Composable fun B(a: (A) -> Unit) { a { B(a) } }
    """
    )

    @Test
    fun testCfromComposableTypeAliasFunInterface() = check(
        """
        import androidx.compose.runtime.Composable

        fun interface A { @Composable fun f() }
        typealias B = A

        @Composable fun C() { A { C() } }
    """
    )

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    fun testComposableLambdaToAll() = check(
        """
        import androidx.compose.runtime.*

        fun foo() {
            val lambda = @Composable { }
            println(lambda)  // println accepts Any, verify no type mismatach.
        }
    """
    )

    @Test
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

    @Test
    fun testNothingAsAValidComposableFunctionBody() = check("""
        import androidx.compose.runtime.*

        val test1: @Composable () -> Unit = TODO()

        @Composable
        fun Test2(): Unit = TODO()

        @Composable
        fun Wrapper(content: @Composable () -> Unit) = content()

        @Composable
        fun Test3() {
            Wrapper {
                TODO()
            }
        }
    """)

    @Test
    fun testComposableValueOperator() {
        check(
            """
            import androidx.compose.runtime.Composable
            import kotlin.reflect.KProperty

            class Foo
            class FooDelegate {
                @Composable
                operator fun getValue(thisObj: Any?, property: KProperty<*>) {}
                @Composable
                operator fun <!COMPOSE_INVALID_DELEGATE!>setValue<!>(thisObj: Any?, property: KProperty<*>, value: Any) {}
            }
            @Composable operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>) {}
            @Composable operator fun Foo.<!COMPOSE_INVALID_DELEGATE!>setValue<!>(thisObj: Any?, property: KProperty<*>, value: Any) {}

            fun <!COMPOSABLE_EXPECTED!>nonComposable<!>() {
                val fooValue = Foo()
                val foo by fooValue
                val fooDelegate by FooDelegate()
                var mutableFoo by <!COMPOSE_INVALID_DELEGATE!>fooValue<!>
                val bar = Bar()

                println(<!COMPOSABLE_INVOCATION!>foo<!>)
                println(<!COMPOSABLE_INVOCATION!>fooDelegate<!>)
                println(bar.<!COMPOSABLE_INVOCATION!>foo<!>)

                <!COMPOSABLE_INVOCATION!>mutableFoo<!> = Unit
            }

            @Composable
            fun TestComposable() {
                val fooValue = Foo()
                val foo by fooValue
                val fooDelegate by FooDelegate()
                val bar = Bar()

                println(foo)
                println(fooDelegate)
                println(bar.foo)
            }

            class Bar {
                val <!COMPOSABLE_EXPECTED!>foo<!> by Foo()

                @get:Composable
                val foo2 by Foo()
            }
            """
        )
    }
}
