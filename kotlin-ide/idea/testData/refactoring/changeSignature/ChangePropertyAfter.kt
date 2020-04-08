open class A {
    open var s: String = 1
}

class AA : A() {
    override var s: String = 1
}

class B : J() {
    override var s: String = 1
}

fun test() {
    with(A()) {
        val t = s
        s = 3
    }

    with(AA()) {
        val t = s
        s = 3
    }

    with(J()) {
        val t = s
        s = 3
    }

    with(B()) {
        val t = s
        s = 3
    }
}