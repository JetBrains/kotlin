// IGNORE_BACKEND: JS
// SPLIT_PER_MODULE
// RUN_PLAIN_BOX_FUNCTION
// EXPECTED_REACHABLE_NODES: 1316

// MODULE: lib1
// FILE: lib1.kt

@JsExport
fun O(): String = "O"

// MODULE: lib2(lib1)
// FILE: lib2.kt

@JsExport
fun K(): String = "K"


// MODULE: main(lib1, lib2)
// FILE: main.kt

@JsExport
fun test() = O() + K()

// FILE: test.js

function box() {

    if (main.test() != "OK") return "fail 1";

    return kotlin_lib1.O() + kotlin_lib2.K();
}
