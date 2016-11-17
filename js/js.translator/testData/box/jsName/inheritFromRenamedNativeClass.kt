// FILE: main.kt

package foo

@native
@JsName("A")
open class B(val foo: String)

class C(s: String) : B(s)

fun box(): String {
    return C("OK").foo
}

// FILE: test.js

function A(foo) {
    this.foo = foo;
}