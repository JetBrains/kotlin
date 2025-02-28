fun box(): String {
    return test1() + test2(1, 2) + test3()
}

fun test1() = expectThrowableMessage {
    assert(object { override fun toString() = "ANONYMOUS" }.toString() == "toString()")
}

open class A

fun test2(a: Int, b: Int) = expectThrowableMessage {
    assert(object : A() {
        fun foo(): Boolean {
            return a > b
        }
        override fun toString() = "ANONYMOUS"
    }.foo())
}

fun test3() = expectThrowableMessage {
    val valueObject = object {
        fun booleanFunction(): Boolean = false
        override fun toString() = "ANONYMOUS"
    }
    assert(valueObject.booleanFunction())
}