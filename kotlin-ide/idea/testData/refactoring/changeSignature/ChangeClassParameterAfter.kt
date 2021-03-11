open class A(open var <caret>s: String)

class B(override var s: String): A(s)

class C: A(0) {
    override var s: String = 1
}

class D(override var s: String) : J()

class E : J() {
    override var s: String = 1
}

fun test() {
    val t1 = A(0).s
    A(0).s = 1

    val t2 = B(0).s
    B(0).s = 2

    val t3 = C().s
    C().s = 3

    val t4 = J().s
    J().s = 4

    val t5 = D().s
    D().s = 5

    val t6 = E().s
    E().s = 6
}