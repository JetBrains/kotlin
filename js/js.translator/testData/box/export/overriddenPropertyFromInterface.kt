// TARGET_BACKEND: JS_IR

interface Foo {
    val foo: String

    val foo2: String

    var foo3: String
}

@JsExport
class Bar : Foo {
    override val foo: String
        get() = "foo"

    override val foo2: String = "foo2"

    override var foo3: String = "foo3"
}

fun box(): String {
    val bar = Bar()
    if (bar.foo != "foo") return "fail 1"
    if (bar.foo2 != "foo2") return "fail 2"
    if (bar.foo3 != "foo3") return "fail 3"
    bar.foo3 = "foo4"
    if (bar.foo3 != "foo4") return "fail 4"

    return "OK"
}