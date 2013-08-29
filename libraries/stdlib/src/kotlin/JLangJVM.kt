package kotlin

import java.lang.Class
import java.lang.Object

import jet.runtime.Intrinsic

[Intrinsic("kotlin.javaClass.property")] public val <T> T.javaClass : Class<T>
    get() = (this as java.lang.Object).getClass() as Class<T>

[Intrinsic("kotlin.javaClass.function")] fun <T> javaClass() : Class<T> = null as Class<T>


/**
 * A helper method for calling hashCode() on Kotlin objects
 * TODO remove when Any supports equals() and hashCode()
 */
[Intrinsic("kotlin.hashCode")] public inline fun Any.hashCode(): Int = (this as java.lang.Object).hashCode()

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Boolean.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Byte.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Short.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Char.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Int.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Float.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Long.toString(): String = (null as String)

/**
 * A helper method for calling toString() on Kotlin primitives
 * TODO remove when Any supports toString()
 */
[Intrinsic("kotlin.toString")] public fun Double.toString(): String = (null as String)
