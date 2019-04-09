package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.r4a.ComposableAnnotationChecker.Mode
import org.jetbrains.kotlin.r4a.R4AComponentRegistrar
import org.jetbrains.kotlin.r4a.AbstractR4aDiagnosticsTest
import org.jetbrains.kotlin.test.KotlinTestUtils

class ComposableCheckerTests : AbstractR4aDiagnosticsTest() {

    companion object {
        val MODE_CHECKED = 1
        val MODE_STRICT = 2
        val MODE_PEDANTIC = 4
        val MODE_ALL = MODE_CHECKED or MODE_STRICT or MODE_PEDANTIC
    }

    fun doTest(mode: Mode, text: String, expectPass: Boolean) {
        val classPath = createClasspath()
        val configuration = KotlinTestUtils.newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.put(R4AComponentRegistrar.COMPOSABLE_CHECKER_MODE_KEY, mode)
        val environment =
            KotlinCoreEnvironment.createForTests(
                myTestRootDisposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        setupEnvironment(environment)

        try {
            doTest(text, environment)
            if (!expectPass)
                throw Exception(
                    "Test unexpectedly passed when running in $mode mode, but SHOULD FAIL"
                )
        } catch (e: Exception) {
            if (expectPass) throw Exception("Unexpected failure while running in $mode mode", e)
        }
    }

    fun doTest(modes: Int, expectedText: String) {
        doTest(Mode.CHECKED, expectedText, (modes and MODE_CHECKED) != 0)
        doTest(Mode.STRICT, expectedText, (modes and MODE_STRICT) != 0)
        doTest(Mode.PEDANTIC, expectedText, (modes and MODE_PEDANTIC) != 0)
    }

    fun testComposableReporting001() {
        doTest(
            MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun myStatelessFunctionalComponent() {
                <TextView text="Hello World!" />
            }

            fun foo() {
                <myStatelessFunctionalComponent />
            }
        """)
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun myStatelessFunctionalComponent() {
                <TextView text="Hello World!" />
            }

            @Composable
            fun foo() {
                <myStatelessFunctionalComponent />
            }
        """)
    }

    fun testComposableReporting002() {
        doTest(MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            val myLambda1 = { <TextView text="Hello World!" /> }
            val myLambda2: ()->Unit = { <TextView text="Hello World!" /> }
        """)
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            val myLambda1 = @Composable() { <TextView text="Hello World!" /> }
            val myLambda2: @Composable() ()->Unit = { <TextView text="Hello World!" /> }
        """)
    }

    fun testComposableReporting003() {
        doTest(
            MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun <!KTX_IN_NON_COMPOSABLE!>myRandomFunction<!>() {
                <TextView text="Hello World!" />
            }
        """)
    }

    fun testComposableReporting004() {
        doTest(
            MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                val myRandomLambda = <!KTX_IN_NON_COMPOSABLE!>{ <TextView text="Hello World!" /> }<!>
                System.out.println(myRandomLambda)
            }
        """)
    }

    fun testComposableReporting005() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                override fun compose() {
                    <TextView text="Hello World!" />
                }
            }
        """)
    }

    fun testComposableReporting006() {
        doTest(
            MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val bar = {
                    <TextView />
                }
                <bar />
                System.out.println(bar)
            }
        """)
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                val bar = @Composable {
                    <TextView />
                }
                <bar />
                System.out.println(bar)
            }
        """)
    }

    fun testComposableReporting007() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo(children: @Composable() ()->Unit) {
                <!SVC_INVOCATION!>children<!>()
            }
        """)
    }

    fun testComposableReporting008() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val bar: @Composable() ()->Unit = @Composable {
                    <TextView />
                }
                <!SVC_INVOCATION!>bar<!>()
                System.out.println(bar)
            }
        """)
    }

    fun testComposableReporting009() {
        doTest(
            MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun myStatelessFunctionalComponent() {
                <TextView text="Hello World!" />
            }

            fun noise() {
                <!SVC_INVOCATION!>myStatelessFunctionalComponent<!>()
            }
        """)
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun myStatelessFunctionalComponent() {
                <TextView text="Hello World!" />
            }

            fun noise() {
                <!SVC_INVOCATION!>myStatelessFunctionalComponent<!>()
            }
        """)
    }

    fun testComposableReporting010() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                @Children lateinit var children: @Composable() ()->Unit
                override fun compose() {
                    val children = this.children
                    <children />
                    System.out.println(children)
                }
            }
        """)
    }

    fun testComposableReporting011() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                @Children lateinit var children: @Composable() ()->Unit
                override fun compose() {
                    <!SVC_INVOCATION!>children<!>()
                }
            }
        """)
    }

    fun testComposableReporting012() {
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                @Children lateinit var children: ()->Unit
                override fun compose() {
                    <!SVC_INVOCATION!>children<!>()
                }
            }
        """)
    }

    fun testComposableReporting013() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                @Children(composable=false) lateinit var children: (value: Int)->Unit
                override fun compose() {
                    children(5)
                }
            }
        """)
    }

    fun testComposableReporting014() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyReceiver {}

            class MyComponent : Component() {
                @Children(composable=false) lateinit var children: MyReceiver.()->Unit
                override fun compose() {
                    MyReceiver().children()
                }
            }
        """)
    }

    fun testComposableReporting015() {
        doTest(MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                override fun compose() {
                    <helper1 />
                    <helper2 />
                }

                fun helper1() {
                    <TextView text="Hello Helper" />
                }

                @Composable
                fun helper2() {
                    <TextView text="Hello Helper" />
                }
            }
        """)
        doTest(
            MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            class MyComponent : Component() {
                override fun compose() {
                    <helper1 />
                    <helper2 />
                }

                fun <!KTX_IN_NON_COMPOSABLE!>helper1<!>() {
                    <TextView text="Hello Helper" />
                }

                @Composable
                fun helper2() {
                    <TextView text="Hello Helper" />
                }
            }
        """)
    }

    fun testComposableReporting016() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;

            <!WRONG_ANNOTATION_TARGET!>@Composable<!>
            class Noise() {}
        """)

        doTest(MODE_ALL, """
            import com.google.r4a.*;

            val adHoc = <!WRONG_ANNOTATION_TARGET!>@Composable()<!> object {
                var x: Int = 0
                var y: Int = 0
            }
        """)

        doTest(MODE_ALL, """
            import com.google.r4a.*;

            open class Noise() {}

            val adHoc = <!WRONG_ANNOTATION_TARGET!>@Composable()<!> object : Noise() {
                var x: Int = 0
                var y: Int = 0
            }
        """)
    }

    fun testComposableReporting017() {
        doTest(
            MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.LinearLayout;
            import android.widget.TextView;

            @Composable
            fun Foo(@Children(composable=false) children: ()->Unit) {
                children()
            }

            @Composable
            fun main() {
                <Foo><TextView text="Hello" /></Foo>
            }
        """)
        doTest(MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.LinearLayout;
            import android.widget.TextView;

            @Composable
            fun Foo(@Children(composable=false) children: ()->Unit) {
                children()
            }

            @Composable
            fun main() {
                <Foo><!KTX_IN_NON_COMPOSABLE!><TextView text="Hello" /><!></Foo>
            }
        """)
    }

    fun testComposableReporting018() {
        doTest(
            MODE_CHECKED or MODE_STRICT, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val myVariable: ()->Unit = @Composable { <TextView text="Hello World!" /> }
                System.out.println(myVariable)
            }
        """)
        doTest(MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val myVariable: ()->Unit = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>@Composable { <TextView text="Hello World!" /> }<!>
                System.out.println(myVariable)
            }
        """)
    }

    fun testComposableReporting019() {
        doTest(MODE_CHECKED or MODE_STRICT, """
           import com.google.r4a.*;
           import android.widget.TextView;

           @Composable
           fun foo() {
               val myVariable: ()->Unit = { }
               <<!NON_COMPOSABLE_INVOCATION!>myVariable<!> />
               System.out.println(myVariable)
           }
        """)
        doTest(MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                val nonComposableLambda: ()->Unit = { }
                <<!NON_COMPOSABLE_INVOCATION!>nonComposableLambda<!> />
                System.out.println(nonComposableLambda)
            }
        """)
    }

    fun testComposableReporting020() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun nonComposableFunction() {}

            @Composable
            fun foo() {
                <<!NON_COMPOSABLE_INVOCATION!>nonComposableFunction<!> />
            }
        """)
    }

    fun testComposableReporting021() {
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach @Composable { value: Int -> <TextView text=value.toString() />; System.out.println(value); }
            }
        """)
    }

    fun testComposableReporting022() {
        doTest(
            MODE_CHECKED, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach { value: Int -> <TextView text=value.toString() />; System.out.println(value); }
            }
        """)
        doTest(
            MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun <!KTX_IN_NON_COMPOSABLE!>foo<!>() {
                val myList = listOf(1,2,3,4,5)
                myList.forEach @Composable { value: Int -> <TextView text=value.toString() />; System.out.println(value); }
            }
        """)
    }

    fun testComposableReporting023() {
        doTest(
            MODE_ALL, """
               import com.google.r4a.*;
               import android.widget.TextView;

               fun foo() {}

               @Composable
               fun bar() {
                    <<!NON_COMPOSABLE_INVOCATION!>foo<!> />
               }
           """)
    }

    fun testComposableReporting024() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;
            import android.widget.LinearLayout;

            fun foo(ll: LinearLayout) {
                ll.composeInto({ <TextView text="Hello World!" /> })
            }
        """)
    }

    fun testComposableReporting025() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                listOf(1,2,3,4,5).forEach { <TextView text="Hello World!" /> }
            }
        """)
    }

    fun testComposableReporting026() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.LinearLayout;
            import android.widget.TextView;

            @Composable
            fun foo() {
                <LinearLayout>
                    <TextView text="Hello Jim!" />
                </LinearLayout>
            }
        """)
    }

    fun testComposableReporting027() {
        doTest(MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.LinearLayout;
            import android.widget.TextView;

            @Composable
            fun foo() {
                <LinearLayout>
                    listOf(1,2,3).forEach {
                        <TextView text="Hello Jim!" />
                    }
                </LinearLayout>
            }
        """)
    }

    fun testComposableReporting028() {
        doTest(
            MODE_CHECKED or MODE_STRICT, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo(v: @Composable() ()->Unit) {
                val myVariable: ()->Unit = v
                myVariable()
            }
        """)
        doTest(MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo(v: @Composable() ()->Unit) {
                val myVariable: ()->Unit = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>v<!>
                myVariable()
            }
        """)
    }

    fun testComposableReporting029() {
        doTest(
            MODE_CHECKED or MODE_STRICT, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo(v: ()->Unit) {
                val myVariable: @Composable() ()->Unit = v;
                <myVariable />
            }
        """)
        doTest(MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo(v: ()->Unit) {
                val myVariable: @Composable() ()->Unit = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>v<!>;
                <myVariable />
            }
        """)
    }

    fun testComposableReporting030() {
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import android.widget.TextView;

            @Composable
            fun foo() {
                val myVariable: @Composable() ()->Unit = {};
                <myVariable />
            }
        """)
    }

    fun testComposableReporting031() {
        doTest(
            MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun foo() {
                val myVariable: ()->Unit = <!KTX_IN_NON_COMPOSABLE!>{ <TextView text="Hello" /> }<!>;
                myVariable();
            }
        """)
    }

    fun testComposableReporting032() {
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import com.google.r4a.Children;
            import android.widget.TextView;

            @Composable
            fun MyComposable(@Children children: ()->Unit) { <children /> }

            @Composable
            fun foo() {
                <MyComposable><TextView text="Hello" /></MyComposable>
            }
        """)
    }

    fun testComposableReporting033() {
        doTest(
            MODE_ALL, """
            import com.google.r4a.*;
            import com.google.r4a.Children;
            import android.widget.TextView;

            @Composable
            fun MyComposable(@Children children: ()->Unit) { <children /> }

            @Composable
            fun foo() {
                <MyComposable children={<TextView text="Hello" />} />
            }
        """)
    }

    fun testComposableReporting034() {
        doTest(
            MODE_CHECKED or MODE_STRICT, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun identity(f: ()->Unit): ()->Unit { return f; }

            @Composable
            fun test(f: @Composable() ()->Unit) {
                val f2: @Composable() ()->Unit = identity(f);
                <f2 />
            }
        """)
        doTest(
            MODE_PEDANTIC, """
            import com.google.r4a.*;
            import android.widget.TextView;

            fun identity(f: ()->Unit): ()->Unit { return f; }

            @Composable
            fun test(f: @Composable() ()->Unit) {
                val f2: @Composable() ()->Unit = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>identity(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>f<!>)<!>;
                <f2 />
            }
        """)
    }

    fun testComposableReporting035() {
        doTest(MODE_PEDANTIC, """
            import com.google.r4a.*

            @Composable
            fun Foo(x: String) {
                @Composable operator fun String.invoke() {}
                <x />
            }
        """)
    }

    fun testComposableReporting036() {
        doTest(MODE_CHECKED, """
            import com.google.r4a.*
            import android.widget.TextView;

            fun Foo() {
                repeat(5) {
                    <TextView text="Hello World" />
                }
            }

            fun Bar() {
                <!SVC_INVOCATION!>Foo<!>()
            }
        """)
        doTest(MODE_CHECKED, """
            import com.google.r4a.*
            import android.widget.TextView;

            fun Foo() {
                repeat(5) {
                    <TextView text="Hello World" />
                }
            }

            fun Bar() {
                <!SVC_INVOCATION!>Foo<!>()
            }
        """)
    }

    fun testComposableReporting037() {
        doTest(MODE_CHECKED, """
            import com.google.r4a.*
            import android.widget.TextView;

            fun Foo() {
                fun Noise() {
                    <TextView text="Hello World" />
                }
            }

            fun Bar() {
                Foo()
            }
        """)
    }

    fun testComposableReporting038() {
        doTest(
            MODE_STRICT or MODE_PEDANTIC, """
            import com.google.r4a.*
            import android.widget.TextView;

            // Function intentionally not inline
            fun repeat(x: Int, l: ()->Unit) { for(i in 1..x) l() }

            fun Foo() {
                repeat(5) <!KTX_IN_NON_COMPOSABLE!>{
                    <TextView text="Hello World" />
                }<!>
            }
        """)
    }

    fun testComposableReporting039() {
        doTest(MODE_CHECKED, """
            import com.google.r4a.*
            import android.widget.TextView;

            fun composeInto(l: @Composable() ()->Unit) { System.out.println(l) }

            fun Foo() {
                composeInto {
                    <TextView text="Hello World" />
                }
            }

            fun Bar() {
                Foo()
            }
        """)
        doTest(MODE_CHECKED, """
            import com.google.r4a.*
            import android.widget.TextView;

            inline fun noise(l: ()->Unit) { l() }

            fun Foo() {
                noise {
                    <TextView text="Hello World" />
                }
            }

            fun Bar() {
                <!SVC_INVOCATION!>Foo<!>()
            }
        """)
    }

    fun testComposableReporting040() {
        doTest(MODE_ALL, """
            import com.google.r4a.*
            import android.widget.TextView;

            @Composable
            fun CraneWrapperComposable(@Children children: () -> Unit) {
                <MyCraneWrapper>
                    <children />
                </MyCraneWrapper>
            }

            class MyCraneWrapper(@Children var children: () -> Unit) : Component() {
                override fun compose() { }
            }
        """)
    }

    fun testComposableReporting041() {
        doTest(MODE_ALL, """
            import com.google.r4a.*
            import android.widget.TextView;

            typealias UNIT_LAMBDA = () -> Unit

            @Composable
            fun CraneWrapperComposable(@Children children: UNIT_LAMBDA) {
                <MyCraneWrapper>
                    <children />
                </MyCraneWrapper>
            }

            class MyCraneWrapper(@Children var children: UNIT_LAMBDA) : Component() {
                override fun compose() { }
            }
        """)
    }
}
