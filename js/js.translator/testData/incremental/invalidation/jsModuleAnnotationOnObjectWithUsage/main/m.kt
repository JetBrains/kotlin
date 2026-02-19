@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val initJsObject = js("""
   globalThis.ObjectB = { getResult: function() { return "OK" } }
""")

fun box(stepId: Int, isWasm: Boolean): String {
    return ObjectB.getResult()
}