// EXPECTED_REACHABLE_NODES: 506
package foo

var c1 = 0
var c2 = 0
var c3 = 0
var c4 = 0
var c5 = 0

fun toStr(): String {
    return "$c1$c2$c3$c4$c5"
}

fun getA(): A? {
    c1++
    return A()
}

fun getNullA(): A? {
    c1++
    return null
}

class A {
    fun someFun(): Int {
        c2++
        return 1
    }
    val b: B
        get() {
            c3++
            return B()
        }
}

fun A.extFun(): Int {
    c4++
    return 3
}

class B {
    operator fun invoke(): Int {
        c5++
        return 2
    }
}

fun box(): String {
    val n1 = getNullA()?.someFun()
    if (n1 != null || toStr() != "10000") {
        return "Bad call getNullA()?.someFun(). result: $n1, counters: ${toStr()}"
    }

    val n2 = getNullA()?.b?.invoke()
    if (n2 != null || toStr() != "20000") {
        return "Bad call getNullA()?.b(). result: $n2, counters: ${toStr()}"
    }

    val n3 = getNullA()?.extFun()
    if (n3 != null || toStr() != "30000") {
        return "Bad call getNullA()?.extFun(). result: $n3, counters: ${toStr()}"
    }

    val i1 = getA()?.someFun()
    if (i1 != 1 || toStr() != "41000") {
        return "Bad call getA()?.someFun(). result: $i1, counters: ${toStr()}"
    }

    val i2 = getA()?.b?.invoke()
    if (i2 != 2 || toStr() != "51101") {
        return "Bad call getA()?.b(). result: $i2, counters: ${toStr()}"
    }

    val i3 = getA()?.extFun()
    if (i3 != 3 || toStr() != "61111") {
        return "Bad call getA()?.extFun(). result: $i3, counters: ${toStr()}"
    }

    return "OK"
}