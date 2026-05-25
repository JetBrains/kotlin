// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

interface Foo {
    @Composable
    fun composableFunction(param: Boolean): Boolean
    fun nonComposableFunction(param: Boolean): Boolean
    val nonComposableProperty: Boolean
}

object FakeFoo : Foo {
    override <!CONFLICTING_OVERLOADS!>fun composableFunction(param: Boolean)<!> = true
    @Composable override <!CONFLICTING_OVERLOADS!>fun nonComposableFunction(param: Boolean)<!> = true
    override val nonComposableProperty: Boolean @Composable <!CONFLICTING_OVERLOADS!>get()<!> = true
}

interface Bar {
    @Composable
    fun composableFunction(param: Boolean): Boolean
    @get:Composable val composableProperty: Boolean
    fun nonComposableFunction(param: Boolean): Boolean
    val nonComposableProperty: Boolean
}

object FakeBar : Bar {
    override <!CONFLICTING_OVERLOADS!>fun composableFunction(param: Boolean)<!> = true
    override <!CONFLICTING_OVERLOADS!>val composableProperty: Boolean<!> = true
    @Composable override <!CONFLICTING_OVERLOADS!>fun nonComposableFunction(param: Boolean)<!> = true
    override val nonComposableProperty: Boolean @Composable <!CONFLICTING_OVERLOADS!>get()<!> = true
}
