class A

operator fun A.<caret>component1(): Int = 0
operator fun A.component2(): Int = 1

fun test() {
    val a = A()
    a.component1()
    val (x, y) = a
}
