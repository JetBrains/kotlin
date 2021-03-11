// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages

interface X

class Y : X

open class G<T> {
    fun get(): T = TODO()
}

abstract class Z1 : List<X>
abstract class Z2 : G<X>()

operator fun X.component1(): Int = 0
operator fun X.<caret>component2(): Int = 1

fun f() = Y()

fun test() {
    val (x, y) = f()
}

fun Y.ext() {
    val (a, b) = this
}

fun g(z1: Z1, z2: Z2) {
    val (x1, y1) = z1[0]
    val (x2, y2) = z2.get()
}
