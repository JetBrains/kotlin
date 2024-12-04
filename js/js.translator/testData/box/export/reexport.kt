// IGNORE_FIR
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: lib1
// FILE: lib1.kt
@JsExport
fun bar() = "O"

// MODULE: lib2(lib1)
// FILE: lib2.kt

@JsExport
fun baz() = "K"

// MODULE: main(lib2)
// FILE: main.kt

@JsExport
fun result(o: String, k: String) = o + k

// FILE: test.js
 function box() {
    const { bar, baz, result } = this.main;
    const o = bar();
    const k = baz();
    return result(o, k);
}