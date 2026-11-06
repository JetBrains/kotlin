// ISSUE: KT-91371

@JsExport
interface A {
    val prop: String
}

interface B : A {
    override val prop: String
}

class BImpl : B {
    override val prop: String = "example"
}

fun box(): String {
    val b: A = BImpl()
    if (b.prop != "example") return "FAIL: ${b.prop}"
    return "OK"
}
