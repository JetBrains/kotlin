// EXPECTED_REACHABLE_NODES: 495
// FILE: foo.kt

package foo

external class A {
    open class B {
        fun foo(): String
    }
}

class C : A.B()

fun box(): String {
    return C().foo()
}

// FILE: bar.js

function A() {
}

A.B = function() {
};

A.B.prototype.foo = function() {
    return "OK"
};