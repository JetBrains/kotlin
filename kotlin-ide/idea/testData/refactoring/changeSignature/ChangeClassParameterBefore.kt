open class A(open var <caret>p: Int)

class B(override var p: Int): A(p)

class C: A(0) {
    override var p: Int = 1
}

class D(override var s: String) : J()

class E : J() {
    override var s: String = 1
}

fun test() {
    val t1 = A(0).p
    A(0).p = 1

    val t2 = B(0).p
    B(0).p = 2

    val t3 = C().p
    C().p = 3

    val t4 = J().p
    J().p = 4

    val t5 = D().p
    D().p = 5

    val t6 = E().p
    E().p = 6
}