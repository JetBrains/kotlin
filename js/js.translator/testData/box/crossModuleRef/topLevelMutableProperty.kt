// EXPECTED_REACHABLE_NODES: 493
// MODULE: lib
// FILE: lib.kt
package lib

var foo = 23

var bar: Int = 42
    get() = field
    set(value) {
        field = value
    }

@JsName("faz") var baz = 99

// MODULE: main(lib)
// FILE: lib.kt
package main

import lib.*

fun box(): String {
    if (foo != 23) return "fail: simple property initial value: $foo"
    foo = 24
    if (foo != 24) return "fail: simple property new value: $foo"

    if (bar != 42) return "fail: property with accessor initial value: $bar"
    bar = 43
    if (bar != 43) return "fail: property with accessor new value: $bar"

    if (baz != 99) return "fail: renamed property initial value: $baz"
    baz = 100
    if (baz != 100) return "fail: renamed property new value: $baz"


    return "OK"
}
