// DUMP_IR

// MODULE: lib
// FILE: p3/foo.kt

package p3;

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
public fun Foo(
    text: @MyInlineable () -> Unit,
) {}

@MyInlineable
public fun FooReturn(
) = @MyInlineable {}

// MODULE: main(lib)
// FILE: main.kt

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.Foo
import p3.FooReturn

@MyInlineable
public fun Bar() {
    Foo(
        text = {}, // @Inlineable invocations can only happen from the context of a @Inlineable function
    )
    FooReturn()()
}

fun box(): String = "OK"
