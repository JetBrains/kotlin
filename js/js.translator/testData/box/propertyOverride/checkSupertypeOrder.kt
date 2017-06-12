// EXPECTED_REACHABLE_NODES: 515
package foo

interface A {
    val bal: Int
        get() {
            return 42
        }
}

open class B {
    open val bal = 239
}

class C1 : B(), A {
    override val bal: Int = 14

    fun getBalA(): Int {
        return super<A>.bal
    }

    fun getBalB(): Int {
        return super<B>.bal
    }
}

class C2 : A, B() {
    override val bal: Int = 14

    fun getBalA(): Int {
        return super<A>.bal
    }

    fun getBalB(): Int {
        return super<B>.bal
    }
}

fun box(): String {
    val c1 = C1();
    if (c1.bal != 14) return "c1.bal != 14, it: ${c1.bal}"
    if (c1.getBalA() != 42) return "c1.getBalA() != 42, it: ${c1.getBalA()}"
    if (c1.getBalB() != 239) return "c1.getBalB() != 239, it: ${c1.getBalB()}"

    val c2 = C2();
    if (c2.bal != 14) return "c2.bal != 14, it: ${c2.bal}"
    if (c2.getBalA() != 42) return "c2.getBalA() != 42, it: ${c2.getBalA()}"
    if (c2.getBalB() != 239) return "c2.getBalB() != 239, it: ${c2.getBalB()}"

    return "OK"
}