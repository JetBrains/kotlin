package foo

open class A {
    open fun f1() = "A1"
    open fun f2() = "A2"
    open fun f3() = "A3"
    open fun f4() = "A4"

    fun getSum(): String {
        return "${f1()}|${f2()}|${f3()}|${f4()}"
    }
}

trait T : A {
    override fun f1() = "T1"
    override fun f2() = "T2"
}

trait B : A {
    override fun f1() = "B1"
    override fun f3() = "B3"
}

trait N : B, T {
    override fun f1() = "N1"
}

trait X {
    fun f4() = "X4"
}

class C : A(), N, X {
    override fun f4() = "C4"
}

fun box(): String {
    val a = A()
    val t = object : T, A() {
    }
    val b = object : B, A() {
    }
    val n = object : N, A() {
    }
    val x = object : X {
    }
    val c = C()

    if (a.getSum() != "A1|A2|A3|A4") return "Bad a.getSum(), it: ${a.getSum()}"
    if (t.getSum() != "T1|T2|A3|A4") return "Bad t.getSum(), it: ${t.getSum()}"
    if (b.getSum() != "B1|A2|B3|A4") return "Bad b.getSum(), it: ${b.getSum()}"
    if (n.getSum() != "N1|T2|B3|A4") return "Bad n.getSum(), it: ${n.getSum()}"
    if (c.getSum() != "N1|T2|B3|C4") return "Bad c.getSum(), it: ${c.getSum()}"

    if (x.f4() != "X4") return "Bad x.f4(), it: ${x.f4()}"
    return "OK"
}