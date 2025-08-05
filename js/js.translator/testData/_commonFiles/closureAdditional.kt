package kotlin

import kotlin.reflect.KClass

public typealias Cloneable = Any
public typealias Void = Unit
public typealias Object = Any
public typealias Class<T> = kotlin.reflect.KClass<T>

inline operator fun Int.Companion.invoke(value: Number) = value.toInt()
inline operator fun String.Companion.invoke(value: String) = value

inline fun Number.intValue() = toInt()

fun String.equalsIgnoreCase(other: String) =
    equals(other)

fun assert(value: Boolean, fn: () -> Any = { "Assertion Exception" }) {
    if (!value) throw Throwable(fn().toString())
}

inline fun <T> synchronized(value: Any?, fn: () -> T) = fn()


val <T> KClass<T>.java: KClass<T> inline get() = this
val <T> KClass<T>.javaPrimitiveType: KClass<T> inline get() = this
val <T> KClass<T>.javaObjectType: KClass<T> inline get() = this.js

val Any.javaClass inline get() = this::class.js

fun <T> Any.isArrayOf() = false