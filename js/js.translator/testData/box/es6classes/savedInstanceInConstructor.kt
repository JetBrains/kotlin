@JsName("Set")
external class JsSet<T> {
    fun has(value: T): Boolean
}

external open class JsFoo(value: String) {
    val value: String
    companion object {
        val instances: JsSet<JsFoo>
    }
}

class KotlinFoo(value: String) : JsFoo(value) {
    fun existsInJs(): Boolean = JsFoo.instances.has(this)
}

fun box(): String {
    val foo = KotlinFoo("TEST")

    assertEquals("TEST", foo.value)
    assertEquals(true, foo.existsInJs())

    return "OK"
}