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

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.AbstractComposeDiagnosticsTest
import org.junit.Test

class ComposableDeclarationCheckerTests : AbstractComposeDiagnosticsTest() {
    @Test
    fun testPropertyWithInitializer() {
        check(
            """
            import androidx.compose.runtime.Composable

            val <!COMPOSABLE_PROPERTY_BACKING_FIELD!>bar<!>: Int = 123
                @Composable get() = field
        """
        )
    }

    @Test
    fun testComposableFunctionReferences() {
        check(
            """
            import androidx.compose.runtime.Composable

            @Composable fun A() {}
            val aCallable: () -> Unit = <!COMPOSABLE_FUNCTION_REFERENCE!>::A<!>
            val bCallable: @Composable () -> Unit = <!COMPOSABLE_FUNCTION_REFERENCE,TYPE_MISMATCH!>::A<!>
            val cCallable = <!COMPOSABLE_FUNCTION_REFERENCE!>::A<!>
            fun doSomething(fn: () -> Unit) { print(fn) }
            @Composable fun B(content: @Composable () -> Unit) {
                content()
                doSomething(<!COMPOSABLE_FUNCTION_REFERENCE!>::A<!>)
                B(<!COMPOSABLE_FUNCTION_REFERENCE,TYPE_MISMATCH!>::A<!>)
            }
        """
        )
    }

    @Test
    fun testNonComposableFunctionReferences() {
        check(
            """
            import androidx.compose.runtime.Composable

            fun A() {}
            val aCallable: () -> Unit = ::A
            val bCallable: @Composable () -> Unit = <!TYPE_MISMATCH!>::A<!>
            val cCallable = ::A
            fun doSomething(fn: () -> Unit) { print(fn) }
            @Composable fun B(content: @Composable () -> Unit) {
                content()
                doSomething(::A)
                B(<!TYPE_MISMATCH!>::A<!>)
            }
        """
        )
    }

    @Test
    fun testPropertyWithGetterAndSetter() {
        check(
            """
            import androidx.compose.runtime.Composable

            var <!COMPOSABLE_VAR!>bam2<!>: Int
                @Composable get() { return 123 }
                set(value) { print(value) }

            var <!COMPOSABLE_VAR!>bam3<!>: Int
                @Composable get() { return 123 }
                <!WRONG_ANNOTATION_TARGET!>@Composable<!> set(value) { print(value) }

            var <!COMPOSABLE_VAR!>bam4<!>: Int
                get() { return 123 }
                <!WRONG_ANNOTATION_TARGET!>@Composable<!> set(value) { print(value) }
        """
        )
    }

    @Test
    fun testPropertyGetterAllForms() {
        check(
            """
            import androidx.compose.runtime.Composable

            val bar2: Int @Composable get() = 123
            @get:Composable val bar3: Int get() = 123

            interface Foo {
                val bar2: Int @Composable get() = 123
                @get:Composable val bar3: Int get() = 123
            }
        """
        )
    }

    @Test
    fun testSuspendComposable() {
        check(
            """
            import androidx.compose.runtime.Composable

            @Composable suspend fun <!COMPOSABLE_SUSPEND_FUN!>Foo<!>() {}

            fun acceptSuspend(fn: suspend () -> Unit) { print(fn) }
            fun acceptComposableSuspend(fn: <!COMPOSABLE_SUSPEND_FUN!>@Composable suspend () -> Unit<!>) { print(fn.hashCode()) }

            val foo: suspend () -> Unit = <!TYPE_MISMATCH!>@Composable {}<!>
            val bar: suspend () -> Unit = {}
            fun Test() {
                val composableLambda = @Composable {}
                acceptSuspend <!TYPE_MISMATCH!>@Composable {}<!>
                acceptComposableSuspend @Composable {}
                acceptComposableSuspend(composableLambda)
                acceptSuspend(<!COMPOSABLE_SUSPEND_FUN,TYPE_MISMATCH!>@Composable suspend fun() { }<!>)
            }
        """
        )
    }

    @Test
    fun testComposableMainFun() {
        check(
            """
            import androidx.compose.runtime.Composable

            @Composable fun <!COMPOSABLE_FUN_MAIN!>main<!>() {}
        """
        )
        check(
            """
            import androidx.compose.runtime.Composable

            @Composable fun <!COMPOSABLE_FUN_MAIN!>main<!>(args: Array<String>) {
                print(args)
            }
        """
        )
        check(
            """
            import androidx.compose.runtime.Composable

            class Foo

            @Composable fun main(foo: Foo) {
                print(foo)
            }
        """
        )
    }

    @Test
    fun testMissingComposableOnOverride() {
        check(
            """
            import androidx.compose.runtime.Composable

            interface Foo {
                @Composable
                fun composableFunction(param: Boolean): Boolean
                fun nonComposableFunction(param: Boolean): Boolean
                val nonComposableProperty: Boolean
            }

            object FakeFoo : Foo {
                <!CONFLICTING_OVERLOADS!>override fun composableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>@Composable override fun nonComposableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>override val nonComposableProperty: Boolean<!> <!CONFLICTING_OVERLOADS!>@Composable get()<!> = true
            }

            interface Bar {
                @Composable
                fun composableFunction(param: Boolean): Boolean
                @get:Composable val composableProperty: Boolean
                fun nonComposableFunction(param: Boolean): Boolean
                val nonComposableProperty: Boolean
            }

            object FakeBar : Bar {
                <!CONFLICTING_OVERLOADS!>override fun composableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>override val composableProperty: Boolean<!> <!CONFLICTING_OVERLOADS!>get()<!> = true
                <!CONFLICTING_OVERLOADS!>@Composable override fun nonComposableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>override val nonComposableProperty: Boolean<!> <!CONFLICTING_OVERLOADS!>@Composable get()<!> = true
            }
        """
        )
    }

    @Test
    fun testInferenceOverComplexConstruct1() {
        check(
            """
            import androidx.compose.runtime.Composable
            val composable: @Composable ()->Unit = if(true) { { } } else { { } }
        """
        )
    }

    @Test
    fun testInferenceOverComplexConstruct2() {
        check(
            """
            import androidx.compose.runtime.Composable
            @Composable fun foo() { }
            val composable: @Composable ()->Unit = if(true) { { } } else { { foo() } }
        """
        )
    }

    @Test
    fun testInterfaceComposablesWithDefaultParameters() {
        check(
            """
            import androidx.compose.runtime.Composable
            interface A {
                @Composable fun foo(x: Int = <!ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE!>0<!>)
            }
        """
        )
    }

    @Test
    fun testAbstractComposablesWithDefaultParameters() {
        check(
            """
            import androidx.compose.runtime.Composable
            abstract class A {
                @Composable abstract fun foo(x: Int = <!ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE!>0<!>)
            }
        """
        )
    }

    @Test
    fun testInterfaceComposablesWithoutDefaultParameters() {
        check(
            """
            import androidx.compose.runtime.Composable
            interface A {
                @Composable fun foo(x: Int)
            }
        """
        )
    }

    @Test
    fun testAbstractComposablesWithoutDefaultParameters() {
        check(
            """
            import androidx.compose.runtime.Composable
            abstract class A {
                @Composable abstract fun foo(x: Int)
            }
        """
        )
    }

    @Test
    fun testOverrideWithoutComposeAnnotation() {
        check(
            """
                import androidx.compose.runtime.Composable
                interface Base {
                    fun compose(content: () -> Unit)
                }

                class Impl : Base {
                    <!CONFLICTING_OVERLOADS!>override fun compose(content: @Composable () -> Unit)<!> {}
                }
            """
        )
    }

    @Test
    fun testOverrideComposableLambda() {
        check(
            """
                import androidx.compose.runtime.Composable

                class Impl : @Composable () -> Unit {
                    @Composable
                    override fun invoke() {}
                }
            """
        )
    }

    @Test
    fun testTransitiveOverrideComposableLambda() {
        check(
            """
                import androidx.compose.runtime.Composable

                interface ComposableFunction : @Composable () -> Unit

                class Impl : ComposableFunction {
                    @Composable
                    override fun invoke() {}
                }
            """
        )
    }

    @Test
    fun testMissingOverrideComposableLambda() {
        check(
            """
                import androidx.compose.runtime.Composable

                class Impl : @Composable () -> Unit {
                    <!CONFLICTING_OVERLOADS!>override fun invoke()<!> {}
                }
            """
        )
    }

    @Test
    fun testWrongOverrideLambda() {
        check(
            """
                import androidx.compose.runtime.Composable

                class Impl : () -> Unit {
                    <!CONFLICTING_OVERLOADS!>@Composable override fun invoke()<!> {}
                }
            """
        )
    }

    @Test
    fun testMultipleOverrideLambda() {
        check(
            """
                import androidx.compose.runtime.Composable

                class Impl : () -> Unit, @Composable (Int) -> Unit {
                    <!CONFLICTING_OVERLOADS!>@Composable override fun invoke()<!> {}
                    @Composable override fun invoke(p0: Int) {}
                }
            """
        )
    }
}
