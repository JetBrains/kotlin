// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1270
// FILE: classes.kt
class C : J

class D : J {
    override fun foo(x: String): String = "D.foo($x)"
}

class E : J {
    override fun bar(x: String): String = "E.bar($x)"
}

class F : L {
    override fun bar(x: String): String = "F.bar($x)"
}

interface I {
    fun foo(x: String = "Q"): String = "I.foo($x)"

    fun bar(x: String = "Q"): String = "I.bar($x)"
}

interface J : I {
    override fun foo(x: String): String = "J.foo($x)"
}

interface K {
    fun foo(x: String = "Q"): String

    fun bar(x: String = "Q"): String
}

interface L : K {
    override fun foo(x: String): String = "L.foo($x)"
}

// FILE: main.kt
// RECOMPILE
fun box(): String {
    var o: I = C()

    var r = o.foo()
    if (r != "J.foo(Q)") return "fail C.foo(): $r"
    r = o.foo("W")
    if (r != "J.foo(W)") return "fail C.foo(W): $r"
    r = o.bar()
    if (r != "I.bar(Q)") return "fail C.bar(): $r"
    r = o.bar("W")
    if (r != "I.bar(W)") return "fail C.bar(W): $r"

    o = D()
    r = o.foo()
    if (r != "D.foo(Q)") return "fail D.foo(): $r"
    r = o.foo("W")
    if (r != "D.foo(W)") return "fail D.foo(W): $r"
    r = o.bar()
    if (r != "I.bar(Q)") return "fail D.bar(): $r"
    r = o.bar("W")
    if (r != "I.bar(W)") return "fail D.bar(W): $r"

    o = E()
    r = o.foo()
    if (r != "J.foo(Q)") return "fail E.foo(): $r"
    r = o.foo("W")
    if (r != "J.foo(W)") return "fail E.foo(W): $r"
    r = o.bar()
    if (r != "E.bar(Q)") return "fail E.bar(): $r"
    r = o.bar("W")
    if (r != "E.bar(W)") return "fail E.bar(W): $r"

    val p: K = F()
    r = p.foo()
    if (r != "L.foo(Q)") return "fail F.foo(): $r"
    r = p.foo("W")
    if (r != "L.foo(W)") return "fail F.foo(W): $r"
    r = p.bar()
    if (r != "F.bar(Q)") return "fail F.bar(): $r"
    r = p.bar("W")
    if (r != "F.bar(W)") return "fail F.bar(W): $r"

    return "OK"
}