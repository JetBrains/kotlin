package foo

open class A {
    open fun foo(a:String,b:String): String = "fooA:" + a + b
}

object B : A() {
    override fun foo(a:String,b:String): String = "fooB:" + a + b
}

fun box(): String {
    var ref = B::foo
    val result = B.(ref)("1", "2")
    return (if (result == "fooB:12") "OK" else result)
}
