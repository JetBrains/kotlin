// EXPECTED_REACHABLE_NODES: 505
package foo

internal external open class A(a: Int) {
    val a: Int

    fun g(): Int = definedExternally
    fun m(): Int = definedExternally

    public open fun foo(i: Int): String = definedExternally
    public fun boo(i: Int): String = definedExternally
    @JsName("bar")
    open fun baz(i: Int): String = definedExternally
}

internal class B(val b: Int) : A(b / 2) {
    override fun foo(i: Int): String = "B.foo($i: Int)"

    fun boo(i: String): String = "B.boo($i: String)"

    fun bar(i: String): String = "B.bar($i: String)"
    override fun baz(i: Int): String = "B.baz($i: Int)"
    fun bar(d: Double): String = "B.bar($d: Double)"
}

fun box(): String {
    val b = B(10)

    if (b !is A) return "b !is A"
    if (b.g() != 10) return "b.g() != 10, it: ${b.g()}"
    if (b.m() != 4) return "b.m() != 4, it: ${b.m()}"

    if (b.foo(4) != "B.foo(4: Int)") return "b.foo(4) != \"B.foo(4: Int)\", it: ${b.foo(4)}"

    if (b.boo(434) != "A.boo(434)") return "b.boo(434) != \"A.boo(434)\", it: ${b.boo(434)}"
    if (b.boo("qlfj") != "B.boo(qlfj: String)") return "b.boo(\"qlfj\") != \"B.boo(qlfj: String)\", it: ${b.boo("qlfj")}"

    if (b.bar("apl") != "B.bar(apl: String)") return "b.bar(\"apl\") != \"B.bar(apl: String)\", it: ${b.bar("apl")}"
    if (b.baz(34) != "B.baz(34: Int)") return "b.baz(34) != \"B.baz(34: Int)\", it: ${b.baz(34)}"
    if (b.bar(2.213) != "B.bar(2.213: Double)") return "b.bar(2.213) != \"B.bar(2.213: Double)\", it: ${b.bar(2.213)}"

    val a: A = b

    if (a.foo(4) != "B.foo(4: Int)") return "a.foo(4) != \"B.foo(4: Int)\", it: ${a.foo(4)}"
    if (a.boo(434) != "A.boo(434)") return "a.boo(434) != \"A.boo(434)\", it: ${a.boo(434)}"
    if (a.baz(34) != "B.baz(34: Int)") return "a.baz(34) != \"B.baz(34: Int)\", it: ${a.baz(34)}"

    return "OK"
}