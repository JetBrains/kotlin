// EXPECTED_REACHABLE_NODES: 1325
// KJS_WITH_FULL_RUNTIME
package foo

external interface EP {
    val simpleProp: Int
    val anotherProp: String
    val propWithGetter: Boolean
}

class P : EP {
    override val simpleProp = 13
    override val anotherProp = "42"
    override val propWithGetter: Boolean
        get() = true
}

fun usages() {
    val p = P()
    assertEquals(13, p.simpleProp)
    assertEquals("42", p.anotherProp)
    assertEquals(true, p.propWithGetter)
}

fun box(): String {
    usages()

    val prototype = P::class.js.asDynamic().prototype

    assertTrue(isConfigurable(prototype, "simpleProp"))
    assertTrue(isConfigurable(prototype, "anotherProp"))
    assertTrue(isConfigurable(prototype, "propWithGetter"))

    return "OK"
}

fun isConfigurable(obj: Any, prop: String): Boolean =
    js("Object")
        .getOwnPropertyDescriptor(obj, prop)
        .configurable
