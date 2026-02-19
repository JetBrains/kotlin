// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// OPT_IN: kotlin.ExperimentalStdlibApi
// FILE: js-symbol-combined.kt

package foo

@JsExport
external interface ExtItf {
    @JsSymbol("iterator")
    fun iter(): dynamic
}

@JsExport
class SymbolHost {
    @JsSymbol("toPrimitive")
    fun convert(hint: String): String = "Converted:$hint"
}

@JsExport
interface SymItf {
    @JsSymbol("toPrimitive")
    fun convert(hint: String): String
}

@JsExport
class SymChild : SymItf {
    override fun convert(hint: String): String = "Child:$hint"
}

@JsExport
abstract class AbsHost {
    @JsSymbol("toPrimitive")
    abstract fun convAbs(hint: String): String
}

@JsExport
class AbsChild : AbsHost() {
    override fun convAbs(hint: String): String = "Abs:$hint"
}

@JsExport
class IterHost : ExtItf {
    override fun iter(): dynamic = js("({ i: 0, next: function() { return this.i < 3 ? { value: ++this.i, done: false } : { value: undefined, done: true }; } })")
}

@JsExport
fun ktBox(): String {
    var res = "OK"
    val sumbolHost = SymbolHost()
    res = sumbolHost.convert("default")
    if (res != "Converted:default") return "Fail toPrimitive: $res"

    val childHost = SymChild()
    res = childHost.convert("X")
    if (res != "Child:X") return "Fail inherited: $res"

    val absHost: AbsHost = AbsChild()
    res = absHost.convAbs("Y")
    if (res != "Abs:Y") return "Fail abstract: $res"

    val iterHost = IterHost()
    val iterator = iterHost.iter()
    if (iterator.next().value != 1) return "Fail iterator 1: the first value is not 1"
    if (iterator.next().value != 2) return "Fail iterator 2: the second value is not 2"
    if (iterator.next().value != 3) return "Fail iterator 3: the third value is not 3"
    if (!iterator.next().done) return "Fail iterator 4: iterator continue to work after it should finish"

    return "OK"
}
