// EXPECTED_REACHABLE_NODES: 1238
// MODULE_KIND: COMMON_JS
// FILE: bar.kt
@file:JsModule("lib")
@file:JsQualifier("foo")
package foo

external interface Bar {
    fun ping(): String
}

// FILE: baz.kt
@file:JsModule("lib")
package boo

external interface Baz {
    fun pong(): Int
}

// FILE: root.kt
@file:JsModule("lib")
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
$kotlin_test_internal$.beginModule();
module.exports = {
    bar : {
        ping() {
            return "ping"
        }
    },
    baz : {
        pong() {
            return 194
        }
    }
};
$kotlin_test_internal$.endModule("lib");
