// DUMP_IR

// MODULE: lib
// FILE: p3/foo.kt

package p3;

import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
public fun Foo(
    text: @MyComposable () -> Unit,
) {}

@MyComposable
public fun FooReturn(
) = @MyComposable {}

// MODULE: main(lib)
// FILE: main.kt

import org.jetbrains.kotlin.fir.plugin.MyComposable
import p3.Foo
import p3.FooReturn

@MyComposable
public fun Bar() {
    Foo(
        text = {}, // @Composable invocations can only happen from the context of a @Composable function
    )
    FooReturn()()
}

fun box(): String = "OK"