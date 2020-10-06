// EXPECTED_REACHABLE_NODES: 1238
// FILE: bar.kt
@file:JsQualifier("foo")
package foo

external interface Bar {
    fun ping(): String
}

// FILE: baz.kt
package boo

external interface Baz {
    fun pong(): Int
}

// FILE: root.kt
import foo.Bar
import boo.Baz

external val bar: Bar
external val baz: Baz

// FILE: test.kt
fun box(): String {
    if (bar.ping() != "ping" || baz.pong() != 194) return "Fail"

    return "OK"
}

// FILE: test.js

var bar = {
    ping() {
        return "ping"
    }
};

var baz = {
    pong() {
        return 194
    }
};
