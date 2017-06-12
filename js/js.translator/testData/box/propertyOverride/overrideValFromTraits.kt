// EXPECTED_REACHABLE_NODES: 512
package foo

interface A {
    val bal: Int
        get() {
            return 5
        }
    var bar: Int
        get() {
            return realBar
        }
        set(value: Int) {
            realBar = value
        }

    var realBar: Int
}


interface B {
    val bal: Int
        get() {
            return 42
        }
    var bar: Int
        get() {
            return realBar + 1000
        }
        set(value: Int) {
            realBar = value + 1000
        }

    var realBar: Int
}

class C : A, B {
    override val bal: Int = 1
    override var bar: Int = 2
        get() {
            return field + 20
        }
        set(value: Int) {
            field = value + 20
        }
    override var realBar: Int = 3

    fun supBalA(): Int {
        return super<A>.bal
    }

    fun supBalB(): Int {
        return super<B>.bal
    }

    fun supBarA(): Int {
        return super<A>.bar
    }

    fun supBarB(): Int {
        return super<B>.bar
    }

    fun setBarA(value: Int) {
        super<A>.bar = value
    }

    fun setBarB(value: Int) {
        super<B>.bar = value
    }
}

fun box(): String {
    val c = C()

    if (c.bal != 1) return "c.bal != 1, it: ${c.bal}"
    if (c.bar != 22) return "c.bar != 22, it: ${c.bar}"
    if (c.realBar != 3) return "c.realBar != 3, it: ${c.realBar}"

    c.bar = 50
    if (c.bar != 90) return "c.bar != 90, it: ${c.bar}"

    if (c.supBalA() != 5) return "c.supBalA() != 5, it: ${c.supBalA()}"
    if (c.supBalB() != 42) return "c.supBalB() != 42, it: ${c.supBalB()}"

    if (c.supBarA() != 3) return "c.supBarA() != 3, it: ${c.supBarA()}"
    if (c.supBarB() != 1003) return "c.supBarB() != 1003, it: ${c.supBarB()}"

    c.setBarA(239)
    if (c.realBar != 239) return "c.realBar != 239, it: ${c.realBar}"
    if (c.supBarA() != 239) return "c.supBarA() != 239, it: ${c.supBarA()}"
    if (c.supBarB() != 1239) return "c.supBarB() != 1239, it: ${c.supBarB()}"

    c.setBarB(239)
    if (c.realBar != 1239) return "c.realBar != 1239, it: ${c.realBar}"
    if (c.supBarA() != 1239) return "c.supBarA() != 1239, it: ${c.supBarA()}"
    if (c.supBarB() != 2239) return "c.supBarB() != 2239, it: ${c.supBarB()}"

    return "OK"
}