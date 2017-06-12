// EXPECTED_REACHABLE_NODES: 495
// MODULE: module1
// FILE: module1.kt

package foo

@JsName("A")
external open class B(foo: String) {
    val foo: String
}

// MODULE: main(module1)
// FILE: main.kt
package foo

class C(s: String) : B(s)

fun box(): String {
    return C("OK").foo
}

// FILE: test.js

function A(foo) {
    this.foo = foo;
}