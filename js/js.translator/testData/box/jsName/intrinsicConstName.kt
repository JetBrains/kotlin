// WITH_STDLIB

@JsExport
object Exported {
    @JsName(" readValue ".trim())
    fun kotlinName(): Int = 41.inc()
}

fun box(): String {
    val exported = Exported.asDynamic()
    return if (exported.readValue() == 42) "OK" else "Fail computed JsName"
}
