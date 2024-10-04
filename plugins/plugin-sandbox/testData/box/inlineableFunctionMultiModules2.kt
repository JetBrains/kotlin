// DUMP_IR

// MODULE: lib
// FILE: p3/foo.kt

package p3;

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun setContent(content: @MyInlineable () -> Unit): Int {
    content()
    return 3
}

// MODULE: main(lib)
// FILE: main.kt

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.setContent

fun test(): Int {
    return setContent {
        Greeting("test")
    }
}

@MyInlineable
fun Greeting(name: String) {
    show("hi $name!")
}

fun show(str: String) {}

fun box(): String = "OK"
