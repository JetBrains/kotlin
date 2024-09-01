abstract class Foo1 {
    abstract fun ok(): String
}

@JsExport
class Bar1 : Foo1() {
    override fun ok(): String {
        return "OK"
    }
}

open class Foo2 {
    open fun ok(): String {
        return "fail"
    }
}

@JsExport
class Bar2 : Foo2() {
    override fun ok(): String {
        return "OK"
    }
}

fun box(): String {
    return if (Bar1().ok() == "OK" && Bar2().ok() == "OK") "OK" else "fail"
}