package foo

open class A {
    open fun foo(a:String,b:String): String = "fooA:" + a + b
}

class B : A() {
    override fun foo(a:String,b:String): String = "fooB:" + a + b
}

fun box(): String {
    val b = B()
    var ref = A::foo
    val result = b.(ref)("1", "2")
    return (if (result == "fooB:12") "OK" else result)
}
