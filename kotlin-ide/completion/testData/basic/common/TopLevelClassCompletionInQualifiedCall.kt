class Test {
    fun foo(a: Collection<String>) {

    }
}

fun test() {
    val t = Test()
    t.foo(Array<caret>)
}

// EXIST: ArrayList
// INVOCATION_COUNT: 2