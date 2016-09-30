package kotlin.js

@native
public val noImpl: Nothing
    get() = throw Exception()

@native
public fun eval(expr: String): dynamic = noImpl

@native
public val undefined: Nothing? = noImpl

@native operator fun <K, V> MutableMap<K, V>.set(key: K, value: V): V? = noImpl

@library
public fun println() {}

@library
public fun println(s: Any?) {}

@library
public fun print(s: Any?) {}

//TODO: consistent parseInt
@native
public fun parseInt(s: String, radix: Int = 10): Int = noImpl

@library
public fun safeParseInt(s: String): Int? = noImpl

@library
public fun safeParseDouble(s: String): Double? = noImpl

@native
public fun js(code: String): dynamic = noImpl

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
public inline fun jsTypeOf(a: Any?): String = js("typeof a")

@library
internal fun deleteProperty(`object`: Any, property: Any): Unit = noImpl