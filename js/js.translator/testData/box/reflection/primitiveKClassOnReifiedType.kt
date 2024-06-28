// EXPECTED_REACHABLE_NODES: 1321

// MODULE: lib
// FILE: lib.kt
inline fun <reified T: Any> klassLib() = T::class

// MODULE: main(lib)
// FILE: main.kt

import kotlin.reflect.KClass

inline fun <reified T: Any> klass() = T::class

fun box(): String {
    check(js("Object"), "Any", klass<Any>())
    check(js("String"), "String", klass<String>())
    check(js("Boolean"), "Boolean", klass<Boolean>())
    check(js("Error"), "Throwable", klass<Throwable>())
    check(js("Array"), "Array", klass<Array<Any>>())
    check(js("Function"), "Function0", klass<Function0<*>>())
    check(js("Function"), "Function1", klass<Function1<*, *>>())

    check(js("Number"), "Byte", klass<Byte>())
    check(js("Number"), "Short", klass<Short>())
    check(js("Number"), "Int", klass<Int>())
    check(js("Number"), "Float", klass<Float>())
    check(js("Number"), "Double", klass<Double>())
    check(js("Number"), "Number", klass<Number>())

    check(js("Array"), "BooleanArray", klass<BooleanArray>())
    check(js("Uint16Array"), "CharArray", klass<CharArray>())
    check(js("Int8Array"), "ByteArray", klass<ByteArray>())
    check(js("Int16Array"), "ShortArray", klass<ShortArray>())
    check(js("Int32Array"), "IntArray", klass<IntArray>())
    check(js("Array"), "LongArray", klass<LongArray>())
    check(js("Float32Array"), "FloatArray", klass<FloatArray>())
    check(js("Float64Array"), "DoubleArray", klass<DoubleArray>())

    // Check same instance
    if (Int::class !== klass<Int>()) return "Same instance check failed"

    // Check inlining from other module works
    check(js("Int8Array"), "ByteArray", klassLib<ByteArray>())

    return "OK"
}

private fun check(nativeClass: dynamic, simpleName: String, c: KClass<*>) {
    assertEquals(simpleName, c.simpleName, "Simple name of class has unexpected value")
    assertEquals(nativeClass, c.js, "Kotlin class does not correspond native class ${nativeClass.name}")
}
