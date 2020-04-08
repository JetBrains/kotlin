class Foo {
    fun add(a: Any) {}
}


fun test() {
    val foo = Foo()
    foo.add(AL<caret>)
}

/* For KT-3779, KT-2821 */

// INVOCATION_COUNT: 2
// EXIST: ArrayList
