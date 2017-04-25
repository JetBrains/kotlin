// EXPECTED_REACHABLE_NODES: 500
import kotlin.reflect.KClass

fun box(): String {
    check(js("Object"), Any::class)
    check(js("String"), String::class)
    check(js("Boolean"), Boolean::class)
    check(js("Error"), Throwable::class)
    check(js("Array"), Array<Any>::class)
    check(js("Function"), Function0::class)

    check(js("Number"), Byte::class)
    check(js("Number"), Short::class)
    check(js("Number"), Int::class)
    check(js("Number"), Float::class)
    check(js("Number"), Double::class)

    check(js("Object"), Any())
    check(js("String"), "*")
    check(js("Boolean"), true)
    check(js("Error"), Throwable())
    check(js("Array"), arrayOf(1, 2, 3))
    check(js("Function"), { x: Int -> x })

    check(js("Number"), 23.toByte())
    check(js("Number"), 23.toShort())
    check(js("Number"), 23)
    check(js("Number"), 23.0F)
    check(js("Number"), 23.0)

    assertEquals("Long", Long::class.simpleName)
    assertEquals("Long", 23L::class.simpleName)
    assertEquals("BoxedChar", Char::class.simpleName)
    assertEquals("BoxedChar", '@'::class.simpleName)
    assertEquals("RuntimeException", RuntimeException::class.simpleName)
    assertEquals("RuntimeException", RuntimeException()::class.simpleName)
    assertEquals("KClass", KClass::class.simpleName)
    assertEquals("KClassImpl", Any::class::class.simpleName)

    return "OK"
}

private fun check(nativeClass: dynamic, c: KClass<*>) {
    assertEquals(null, c.simpleName, "Simple name of native class ${nativeClass.name} must be null")
    assertEquals(nativeClass, c.js, "Kotlin class does not correspond native class ${nativeClass.name}")
}

private fun check(nativeClass: dynamic, value: Any) {
    check(nativeClass, value::class)
}