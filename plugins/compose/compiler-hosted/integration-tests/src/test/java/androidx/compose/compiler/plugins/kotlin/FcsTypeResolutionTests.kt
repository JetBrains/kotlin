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

import org.junit.Test

class FcsTypeResolutionTests : AbstractComposeDiagnosticsTest() {
    @Test
    fun testImplicitlyPassedReceiverScope1() = check(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Int.Foo(content: @Composable Int.() -> Unit) {
                content()
            }
        """
    )

    @Test
    fun testImplicitlyPassedReceiverScope2() = check(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Int.Foo(content: @Composable Int.(foo: String) -> Unit) {
                content<!NO_VALUE_FOR_PARAMETER!>()<!>
            }

            @Composable
            fun Bar(content: @Composable Int.() -> Unit) {
                content<!NO_VALUE_FOR_PARAMETER!>()<!>
            }
        """
    )

    @Test
    fun testSmartCastsAndPunning() = check(
        """
            import androidx.compose.runtime.*

            @Composable
            fun Foo(bar: String) { print(bar) }

            @Composable
            fun test(bar: String?) {
                Foo(<!TYPE_MISMATCH!>bar<!>)
                if (bar != null) {
                    Foo(bar)
                    Foo(bar=bar)
                }
            }
        """
    )

    @Test
    fun testExtensionInvoke() = check(
        """
            import androidx.compose.runtime.*

            class Foo {}
            @Composable operator fun Foo.invoke() {}

            @Composable fun test() {
                Foo()
            }
        """
    )

    @Test
    fun testResolutionInsideWhenExpression() = check(
        """
            import androidx.compose.runtime.*
            
            @Composable fun TextView(text: String) { print(text) }

            @Composable fun doSomething(foo: Boolean) {
                when (foo) {
                    true -> TextView(text="Started...")
                    false -> TextView(text="Continue...")
                }
            }
        """
    )

    @Test
    fun testUsedParameters() = check(
        """
            import androidx.compose.runtime.*
            import android.widget.LinearLayout

            @Composable fun Foo(x: Int, composeItem: @Composable () -> Unit = {}) {
                println(x)
                print(composeItem == {})
            }

            @Composable fun test(
                content: @Composable () -> Unit,
                value: Int,
                x: Int,
                content2: @Composable () -> Unit,
                value2: Int
            ) {
                Foo(123) {
                    // named argument
                    Foo(x=value)

                    // indexed argument
                    Foo(x)

                    // tag
                    content()
                }
                Foo(x=123, composeItem={
                    val abc = 123

                    // attribute value
                    Foo(x=abc)

                    // attribute value
                    Foo(x=value2)

                    // tag
                    content2()
                })
            }
        """
    )

    @Test
    fun testDispatchInvoke() = check(
        """
            import androidx.compose.runtime.*

            class Bam {
                @Composable fun Foo() {}
            }

            @Composable fun test() {
                with(Bam()) {
                    Foo()
                }
            }
        """
    )

    @Test
    fun testDispatchAndExtensionReceiver() = check(
        """
            import androidx.compose.runtime.*

            class Bam {
                inner class Foo {}
            }

            @Composable operator fun Bam.Foo.invoke() {}

            @Composable fun test() {
                with(Bam()) {
                    Foo()
                }
            }
        """
    )

    @Test
    fun testDispatchAndExtensionReceiverLocal() = check(
        """
            import androidx.compose.runtime.*

            class Foo {}

            class Bam {
                @Composable operator fun Foo.invoke() {}
                @Composable operator fun invoke() {
                    Foo()
                }
            }

        """
    )

    @Test
    fun testMissingAttributes() = check(
        """
            import androidx.compose.runtime.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }

            // NOTE: It's important that the diagnostic be only over the call target, and not the
            // entire element so that a single error doesn't end up making a huge part of an 
            // otherwise correct file "red".
            @Composable fun Test(F: @Composable (x: Foo) -> Unit) {
                // NOTE: constructor attributes and fn params get a "missing parameter" diagnostic
                A<!NO_VALUE_FOR_PARAMETER!>()<!>

                // local
                F<!NO_VALUE_FOR_PARAMETER!>()<!>

                val x = Foo(123)

                A(x)
                F(x)
                A(x=x)
                F(x=x)
            }

        """.trimIndent()
    )

    @Test
    fun testDuplicateAttributes() = check(
        """
            import androidx.compose.runtime.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }

            @Composable fun Test() {
                val x = Foo(123)

                // NOTE: It's important that the diagnostic be only over the attribute key, so that
                // we don't make a large part of the elements red when the type is otherwise correct
                A(x=x, <!ARGUMENT_PASSED_TWICE!>x<!>=x)
            }

        """.trimIndent()
    )

    @Test
    fun testChildrenNamedAndBodyDuplicate() = check(
        """
            import androidx.compose.runtime.*

            @Composable fun A(content: @Composable () -> Unit) { content() }

            @Composable fun Test() {
                A(content={}) <!TOO_MANY_ARGUMENTS!>{ }<!>
            }

        """.trimIndent()
    )

    @Test
    fun testAbstractClassTags() = check(
        """
            import androidx.compose.runtime.*
            import android.content.Context
            import android.widget.LinearLayout

            abstract class Foo {}

            abstract class Bar(context: Context) : LinearLayout(context) {}

            @Composable fun Test() {
                <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Foo()<!>
                <!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Bar<!NO_VALUE_FOR_PARAMETER!>()<!><!>
            }

        """.trimIndent()
    )

    @Test
    fun testGenerics() = check(
        """
            import androidx.compose.runtime.*

            class A { fun a() {} }
            class B { fun b() {} }

            @Composable fun <T> Bar(x: Int, value: T, f: (T) -> Unit) { println(value); println(f); println(x) }

            @Composable fun Test() {

                val fa: (A) -> Unit = { it.a() }
                val fb: (B) -> Unit = { it.b() }

                Bar(x=1, value=A(), f={ it.a() })
                Bar(x=1, value=B(), f={ it.b() })
                Bar(x=1, value=A(), f=fa)
                Bar(x=1, value=B(), f=fb)
                Bar(x=1, value=B(), f={ it.<!UNRESOLVED_REFERENCE!>a<!>() })
                Bar(x=1, value=A(), f={ it.<!UNRESOLVED_REFERENCE!>b<!>() })
                Bar(
                  x=1, 
                  value=A(), 
                  f=<!TYPE_MISMATCH!>fb<!>
                )
                Bar(
                  x=1,
                  value=B(), 
                  f=<!TYPE_MISMATCH!>fa<!>
                )
            }

        """.trimIndent()
    )

    @Test
    fun testUnresolvedAttributeValueResolvedTarget() = check(
        """
            import androidx.compose.runtime.*

            @Composable fun Fam(bar: Int, x: Int) {
                print(bar)
                print(x)
            }

            @Composable fun Test() {
                Fam(
                  bar=<!UNRESOLVED_REFERENCE!>undefined<!>,
                  x=1
                )
                Fam(
                  bar=1,
                  x=<!UNRESOLVED_REFERENCE!>undefined<!>
                )
                Fam(
                  <!UNRESOLVED_REFERENCE!>bar<!>,
                  <!UNRESOLVED_REFERENCE!>x<!>
                )

                Fam(
                  bar=<!TYPE_MISMATCH!>""<!>,
                  x=<!TYPE_MISMATCH!>""<!>
                )
            }

        """.trimIndent()
    )

    // TODO(lmr): this triggers an exception!
    @Test
    fun testEmptyAttributeValue() = check(
        """
            import androidx.compose.runtime.*

            @Composable fun Foo(abc: Int, xyz: Int) {
                print(abc)
                print(xyz)
            }

            @Composable fun Test() {
                Foo(<!NO_VALUE_FOR_PARAMETER!>abc=)<!>

                // NOTE(lmr): even though there is NO diagnostic here, there *is* a parse
                // error. This is intentional and done to mimic how kotlin handles function
                // calls with no value expression in a call parameter list (ie, `Foo(123,)`)
                Foo(abc=123, xyz=)
            }

        """.trimIndent(),
        ignoreParseErrors = true
    )

    @Test
    fun testMismatchedAttributes() = check(
        """
            import androidx.compose.runtime.*

            open class A {}
            class B : A() {}

            @Composable fun Foo(x: A = A(), y: A = B(), z: B = B()) {
                print(x)
                print(y)
                print(z)
            }

            @Composable fun Test() {
                Foo(
                    x=A(),
                    y=A(),
                    z=<!TYPE_MISMATCH!>A()<!>
                )
                Foo(
                    x=B(),
                    y=B(),
                    z=B()
                )
                Foo(
                    x=<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>,
                    y=<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>,
                    z=<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
                )
            }

        """.trimIndent()
    )

    @Test
    fun testErrorAttributeValue() = check(
        """
            import androidx.compose.runtime.*

            @Composable fun Foo(x: Int = 1) { print(x) }

            @Composable fun Test() {
                Foo(
                    x=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>,
                    <!NAMED_PARAMETER_NOT_FOUND!>y<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
                )
            }

        """.trimIndent()
    )

    @Test
    fun testUnresolvedQualifiedTag() = check(
        """
            import androidx.compose.runtime.*

            object MyNamespace {
                @Composable fun Bar(content: @Composable () -> Unit = {}) { 
                    content() 
                }

                var Baz = @Composable { }

                var someString = ""
                class NonComponent {}
            }

            class Boo {
                @Composable fun Wat() { }
            }

            @Composable fun Test() {

                MyNamespace.Bar()
                MyNamespace.Baz()
                MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!>()
                MyNamespace.<!FUNCTION_EXPECTED!>someString<!>()
                MyNamespace.NonComponent()
                MyNamespace.Bar {}
                MyNamespace.Baz <!TOO_MANY_ARGUMENTS!>{}<!>

                val obj = Boo()
                Boo.<!UNRESOLVED_REFERENCE!>Wat<!>()
                obj.Wat()

                MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!>()
                <!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo()

                obj.Wat <!TOO_MANY_ARGUMENTS!>{
                }<!>

                MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!> {
                }

                MyNamespace.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>someString<!> {
                }

                <!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo {
                }

                MyNamespace.NonComponent <!TOO_MANY_ARGUMENTS!>{}<!>

                MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!> {}

            }

        """.trimIndent()
    )

    // TODO(lmr): overloads creates resolution exception
    @Test
    fun testChildren() = check(
        """
            import androidx.compose.runtime.*
            import android.widget.Button
            import android.widget.LinearLayout

            @Composable fun ChildrenRequired2(content: @Composable () -> Unit) { content() }

            @Composable fun ChildrenOptional3(content: @Composable () -> Unit = {}){ content() }

            @Composable fun NoChildren2() {}

            @Composable 
            fun MultiChildren(c: @Composable (x: Int) -> Unit = {}) { c(1) }

            @Composable 
            fun MultiChildren(c: @Composable (x: Int, y: Int) -> Unit = { x, y ->println(x + y) }) { c(1,1) }

            @Composable fun Test() {
                ChildrenRequired2 {}
                ChildrenRequired2<!NO_VALUE_FOR_PARAMETER!>()<!>

                ChildrenOptional3 {}
                ChildrenOptional3()

                NoChildren2 <!TOO_MANY_ARGUMENTS!>{}<!>
                NoChildren2()

                <!OVERLOAD_RESOLUTION_AMBIGUITY!>MultiChildren<!> {}
                MultiChildren { x ->
                    println(x)
                }
                MultiChildren { x, y ->
                    println(x + y)
                }
                <!NONE_APPLICABLE!>MultiChildren<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!>,
                <!CANNOT_INFER_PARAMETER_TYPE!>y<!>, <!CANNOT_INFER_PARAMETER_TYPE!>z<!> ->
                    println(x + y + z)
                }
            }

        """.trimIndent()
    )
}
