// EXPECTED_REACHABLE_NODES: 632
package foo

class C(val i: Int) : Comparable<C>, A() {
    public override fun compareTo(other: C): Int {
        return if (other is C) other.i - i else 0
    }

}

operator fun ClosedRange<C>.iterator(): Iterator<C> {
    var curI: Int = start.i - 1

    return object : Iterator<C> {
        public override fun next(): C {
            curI++
            return C(curI)
        }
        public override fun hasNext(): Boolean {
            return curI <= endInclusive.i
        }

    }
}

open class A {
    operator fun component1(): Int = 1
}
operator fun A.component2(): String = "n"

fun box(): String {
    var i = 0;
    var s = ""
    for ((a, b) in C(0)..C(2)) {
        i = a;
        s = b;
    }

    if (i != 1) return "i != 1, it: " + i
    if (s != "n") return "s != 'n', it: " + s

    return "OK"
}