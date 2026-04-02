// MODULE: lib
// FILE: lib.kt
inline fun <reified T: Any> klassLib() = T::class

// MODULE: main(lib)
// FILE: main.kt

import kotlin.reflect.KClass

inline fun <reified T: Any> klass() = T::class

fun compileLongAsBigInt(): Boolean {
    val long = 1L
    return js("typeof long === 'bigint'").unsafeCast<Boolean>()
}

fun box(): String {
    check(js("Object"), "Any", klass<Any>(), Any::class.js)
    check(js("String"), "String", klass<String>(), String::class.js)
    check(js("Boolean"), "Boolean", klass<Boolean>(), Boolean::class.js)
    check(js("Error"), "Throwable", klass<Throwable>(), Throwable::class.js)
    check(js("Array"), "Array", klass<Array<Any>>(), Array::class.js)
    check(js("Function"), "Function0", klass<Function0<*>>(), Function0::class.js)
    check(js("Function"), "Function1", klass<Function1<*, *>>(), Function1::class.js)

    check(js("Number"), "Byte", klass<Byte>(), Byte::class.js)
    check(js("Number"), "Short", klass<Short>(), Short::class.js)
    check(js("Number"), "Int", klass<Int>(), Int::class.js)
    check(js("Number"), "Float", klass<Float>(), Float::class.js)
    check(js("Number"), "Double", klass<Double>(), Double::class.js)
    check(js("Number"), "Number", klass<Number>(), Number::class.js)

    check(js("Array"), "BooleanArray", klass<BooleanArray>(), BooleanArray::class.js)
    check(js("Uint16Array"), "CharArray", klass<CharArray>(), CharArray::class.js)
    check(js("Int8Array"), "ByteArray", klass<ByteArray>(), ByteArray::class.js)
    check(js("Int16Array"), "ShortArray", klass<ShortArray>(), ShortArray::class.js)
    check(js("Int32Array"), "IntArray", klass<IntArray>(), IntArray::class.js)

    if (compileLongAsBigInt()) {
        check(js("BigInt64Array"), "LongArray", klass<LongArray>(), LongArray::class.js)
    } else {
        check(js("Array"), "LongArray", klass<LongArray>(), LongArray::class.js)
    }

    check(js("Float32Array"), "FloatArray", klass<FloatArray>(), FloatArray::class.js)
    check(js("Float64Array"), "DoubleArray", klass<DoubleArray>(), DoubleArray::class.js)

    // Check same instance
    if (Int::class !== klass<Int>()) return "Same instance check failed"

    // Check inlining from other module works
    check(js("Int8Array"), "ByteArray", klassLib<ByteArray>(), ByteArray::class.js)

    return "OK"
}

private fun check(nativeClass: dynamic, simpleName: String, c: KClass<*>, jsClass: JsClass<*>) {
    assertEquals(simpleName, c.simpleName, "Simple name of class has unexpected value")
    assertEquals(nativeClass, c.js, "Kotlin class does not correspond native class ${nativeClass.name}")
    assertEquals(c.js, jsClass, "Native class (gotten by getKClass().js) does not correspond to native class gotten by optimization of ::class.js: ${nativeClass?.name}")
}
