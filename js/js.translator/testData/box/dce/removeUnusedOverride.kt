// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: remove_unused_override
// FILE: lib.kt

interface I {
    fun foo() = "OK"
}

abstract class A : I

class B : A()

class C : A() {
    override fun foo(): String {
        return "C::foo"
    }
}

@JsExport
fun makeB(): A {
    val b = B()
    b.foo()
    return b
}

@JsExport
fun makeC(): A {
    return C()
}

// FILE: test.js
function box() {
    var b = this["remove_unused_override"].makeB()
    var c = this["remove_unused_override"].makeC()

    var cnt = 0
    for(var prop in b) {
        if (typeof b[prop] === 'function' && prop != 'constructor') {
            ++cnt
            if (b[prop].call(b) != "OK") return "expect inherited method"
        }
    }
    for(var prop in c) {
        if (typeof c[prop] === 'function' && prop != 'constructor') {
            ++cnt
            if (c[prop].call(c) != "OK") return "override C::foo() should be removed by DCE"
        }
    }

    return cnt == 2 ? "OK" : ("Expected 2 calls, got " + cnt)
}
