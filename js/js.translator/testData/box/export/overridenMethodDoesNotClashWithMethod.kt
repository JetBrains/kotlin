@JsExport
abstract class Foo1 {
    abstract fun ok(a: Int): String
}

open class Foo2 : Foo1() {
    override fun ok(a: Int): String {
        return "OK"
    }
}

class Foo3 : Foo2() {
    override fun ok(a: Int): String {
        return "OK"
    }

    fun ok(): String {
        return "OK"
    }
}

fun box(): String {
    return if (Foo2().ok(1) == "OK" && Foo3().ok() == "OK" && Foo3().ok(1) == "OK") "OK" else "fail"
}
