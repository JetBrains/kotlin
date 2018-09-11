// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1304
// MODULE: lib1
// FILE: lib1.kt
package pkg

object O {
    fun f() = "O.f"
}

fun foo() = "foo"

class A {
    fun f() = "A.f"
}

// MODULE: lib2
// FILE: lib2.kt
package pkg

object P {
    fun f() = "P.f"
}

fun bar() = "bar"

class B {
    fun f() = "B.f"
}

// MODULE: main(lib1, lib2)
// FILE: main.kt
import pkg.*

fun box(): String {
    var r = O.f()
    if (r != "O.f") return "fail1: $r"

    r = foo()
    if (r != "foo") return "fail2: $r"

    r = A().f()
    if (r != "A.f") return "fail3: $r"

    r = P.f()
    if (r != "P.f") return "fail4: $r"

    r = bar()
    if (r != "bar") return "fail5: $r"

    r = B().f()
    if (r != "B.f") return "fail6: $r"

    return "OK"
}