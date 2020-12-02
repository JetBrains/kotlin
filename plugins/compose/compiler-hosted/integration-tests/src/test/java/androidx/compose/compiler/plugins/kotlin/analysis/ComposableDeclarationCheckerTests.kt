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

/**
 * We're strongly considering supporting try-catch-finally blocks in the future.
 * If/when we do support them, these tests should be deleted.
 */
class ComposableDeclarationCheckerTests : AbstractComposeDiagnosticsTest() {
    fun testPropertyWithInitializer() {
        doTest(
            """
            import androidx.compose.runtime.Composable

            @Composable
            val <!DEPRECATED_COMPOSABLE_PROPERTY,COMPOSABLE_PROPERTY_BACKING_FIELD!>foo<!>: Int = 123

            val <!COMPOSABLE_PROPERTY_BACKING_FIELD!>bar<!>: Int = 123
                @Composable get() = field
        """
        )
    }

    fun testComposableFunctionReferences() {
        doTest(
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

    fun testNonComposableFunctionReferences() {
        doTest(
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

    fun testPropertyWithJustGetter() {
        doTest(
            """
            import androidx.compose.runtime.Composable

            @Composable
            val <!DEPRECATED_COMPOSABLE_PROPERTY!>bar<!>: Int get() = 123

            val foo: Int @Composable get() = 123
        """
        )
    }

    fun testPropertyWithGetterAndSetter() {
        doTest(
            """
            import androidx.compose.runtime.Composable

            @Composable
            var <!DEPRECATED_COMPOSABLE_PROPERTY, COMPOSABLE_VAR!>bam<!>: Int
                get() { return 123 }
                set(value) { print(value) }

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

    fun testPropertyGetterAllForms() {
        doTest(
            """
            import androidx.compose.runtime.Composable

            @Composable val <!DEPRECATED_COMPOSABLE_PROPERTY!>bar1<!>: Int get() = 123
            val bar2: Int @Composable get() = 123
            @get:Composable val bar3: Int get() = 123

            interface Foo {
                @Composable val <!DEPRECATED_COMPOSABLE_PROPERTY!>bar1<!>: Int get() = 123
                val bar2: Int @Composable get() = 123
                @get:Composable val bar3: Int get() = 123
            }
        """
        )
    }

    fun testMarkedPropInOverrideMarkedGetter() {
        doTest(
            """
            import androidx.compose.runtime.Composable
            interface A {
                val foo: Int @Composable get() = 123
            }
            class Impl : A {
                @Composable override val <!DEPRECATED_COMPOSABLE_PROPERTY!>foo<!>: Int get() = 123
            }
        """
        )
    }

    fun testMarkedGetterInOverrideMarkedProp() {
        doTest(
            """
            import androidx.compose.runtime.Composable
            interface A {
                @Composable val <!DEPRECATED_COMPOSABLE_PROPERTY!>foo<!>: Int get() = 123
            }
            class Impl : A {
                override val foo: Int @Composable get() = 123
            }
        """
        )
    }

    fun testSuspendComposable() {
        doTest(
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
                acceptComposableSuspend(<!UNSUPPORTED_FEATURE!>composableLambda<!>)
                acceptSuspend(<!COMPOSABLE_SUSPEND_FUN, TYPE_MISMATCH!>@Composable suspend fun() { }<!>)
            }
        """
        )
    }

    fun testMissingComposableOnOverride() {
        doTest(
            """
            import androidx.compose.runtime.Composable

            interface Foo {
                @Composable
                fun composableFunction(param: Boolean): Boolean
                @Composable
                val <!DEPRECATED_COMPOSABLE_PROPERTY!>composableProperty<!>: Boolean
                fun nonComposableFunction(param: Boolean): Boolean
                val nonComposableProperty: Boolean
            }

            object FakeFoo : Foo {
                <!CONFLICTING_OVERLOADS!>override fun composableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>override val composableProperty: Boolean<!> <!CONFLICTING_OVERLOADS!>get()<!> = true
                <!CONFLICTING_OVERLOADS!>@Composable override fun nonComposableFunction(param: Boolean)<!> = true
                <!CONFLICTING_OVERLOADS!>@Composable override val <!DEPRECATED_COMPOSABLE_PROPERTY!>nonComposableProperty<!>: Boolean<!> <!CONFLICTING_OVERLOADS!>get()<!> = true
            }

            interface Bar {
                @Composable
                fun composableFunction(param: Boolean): Boolean
                val composableProperty: Boolean @Composable get()
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
}
