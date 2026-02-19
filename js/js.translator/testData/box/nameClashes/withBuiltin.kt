interface MarkerInterface {
    class Symbol

    companion object {
        @JsStatic val Symbol: String = "OK"
    }
}

open class C : MarkerInterface {
    private fun constructor() = "C.constructor"
    fun constructor(value: String) = value

    fun f(): String = constructor()
}

class D : C() {
    class `$metadata$`
}

fun box(): String {
    val d = D()

    val x = d.f()
    if (x != "C.constructor") return "fail1: $x"

    if (x.asDynamic().constructor === D::class.js) return "fail2"

    if (d.constructor("OK") != "OK") return "fail3: public function overrides built-in"

    if (d !is MarkerInterface) return "fail4: special static field Symbol is overridden"

    if (MarkerInterface.Companion.Symbol != "OK") return "fail5: renaming of statics doesn't work"

    if (jsTypeOf(D.`$metadata$`::class.js) !== "function") return "fail6: special static field `\$metadata$` is overridden"

    return "OK"
}