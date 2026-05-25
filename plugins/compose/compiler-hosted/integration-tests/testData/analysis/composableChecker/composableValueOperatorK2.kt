// RUN_PIPELINE_TILL: FRONTEND

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

fun <!COMPOSABLE_EXPECTED, COMPOSABLE_EXPECTED, COMPOSABLE_EXPECTED, COMPOSABLE_EXPECTED!>nonComposable<!>() {
    val fooValue = Foo()
    val foo by fooValue
    val fooDelegate by FooDelegate()
    var <!COMPOSE_INVALID_DELEGATE, COMPOSE_INVALID_DELEGATE!>mutableFoo<!> by fooValue
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
