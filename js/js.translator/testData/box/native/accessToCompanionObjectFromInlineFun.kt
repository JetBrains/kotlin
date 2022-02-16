// V8 fail: https://bugs.chromium.org/p/v8/issues/detail?id=12834
// IGNORE_BACKEND: WASM

// EXPECTED_REACHABLE_NODES: 1281
// FILE: main.kt

package foo

external class B {
    companion object {
        val value: String
    }
}

inline fun test() = B.value

fun box(): String {
    return test()
}

// FILE: native.js

function B() {};

B.value = "OK";
