fun box(): String = runAll(
    "test1" to { test1() },
    "test2" to { test2(1, 2) },
    "test3" to { test3() },
)

fun test1() {
    assert(object { override fun toString() = "ANONYMOUS" }.toString() == "toString()")
}

open class A

fun test2(a: Int, b: Int) {
    assert(object : A() {
        fun foo(): Boolean {
            return a > b
        }
        override fun toString() = "ANONYMOUS"
    }.foo())
}

fun test3() {
    val valueObject = object {
        fun booleanFunction(): Boolean = false
        override fun toString() = "ANONYMOUS"
    }
    assert(valueObject.booleanFunction())
}