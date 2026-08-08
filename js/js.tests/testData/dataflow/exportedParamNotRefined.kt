// Soundness: exported function parameters must not be refined from call sites.

interface Base
class Child : Base

@JsExport
fun take(p: Base, n: Int): String = "OK"

fun box(): String = take(Child(), 7)
