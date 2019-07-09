// EXPECTED_REACHABLE_NODES: 1203

external interface Foo {
    var externalProperty: String?
        get() = definedExternally
        set(it) = definedExternally
}

interface Bar : Foo

class CCC: Foo

class DDD: Bar

interface Bar2: Foo {
    override var externalProperty: String?
        get() = "Bar2"
        set(value) {}
}

class FFF: Bar2

fun box(): String {
    val c = CCC()
    if (c.externalProperty != null) return "fail1"
    val d = DDD()
    if (d.externalProperty != null) return "fail2"
    val f = FFF()
    if (f.externalProperty != "Bar2") return "fail3"
    return "OK"
}