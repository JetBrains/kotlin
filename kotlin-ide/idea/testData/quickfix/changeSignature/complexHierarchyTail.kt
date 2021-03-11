// "Add parameter to function 'f'" "true"
interface OA {
    fun f(a: Int)
}

interface OB {
    fun f(a: Int)
}

interface O : OA, OB {
    override fun f(a: Int)
}

interface OO : O {
    override fun f(a: Int) {
    }
}

interface OOO : OO {
    override fun f(a: Int) {}
}

interface OOOA : OOO {
    override fun f(a: Int) {
    }
}

interface OOOB : OOO {
    override fun f(a: Int) {
    }
}

fun usage(o: OA) {
    o.f(1)
}
fun usage(o: OB) {
    o.f(1)
}

fun usage(o: O) {
    o.f(1)
}

fun usage(o: OO) {
    o.f(13)
}

fun usage(o: OOO) {
    o.f(3)
}

fun usage(o: OOOA) {
    o.f(3)
}

fun usage(o: OOOB) {
    o.f(3, <caret>"asdv")
}
