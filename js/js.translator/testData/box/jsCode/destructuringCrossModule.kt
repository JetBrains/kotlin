// MODULE: lib
// FILE: lib.kt
package lib

fun destructureObject(payload: dynamic): Int =
    js(
        "(function () { const key = 'c'; const { a, nested: { b }, [key]: c = 3 } = payload; return a + b + c; })()"
    )

fun destructureArray(payload: dynamic): Int =
    js("(function () { const [head, [middle], ...tail] = payload; return head + middle + tail.length; })()")

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.destructureArray
import lib.destructureObject

fun box(): String {
    val objectResult = destructureObject(js("{ a: 1, nested: { b: 2 } }"))
    if (objectResult != 6) return "fail object destructuring: $objectResult"

    val arrayResult = destructureArray(js("[1, [2], 3, 4]"))
    if (arrayResult != 5) return "fail array destructuring: $arrayResult"

    return "OK"
}
