// KT-53968

@JsExport
class LateinitContainer {
    lateinit var value: String;
}

fun box(): String {
    val container = LateinitContainer()
    try {
        container.value
        return "Fail: problem with lateinit getter."
    } catch (e: Exception) {}

    container.value = "Test"
    assertEquals(container.value, "Test")
    return "OK"
}