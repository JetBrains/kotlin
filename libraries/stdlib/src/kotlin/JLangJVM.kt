package kotlin

import java.lang.Class
import java.lang.Object
import java.lang.annotation.*

import jet.runtime.Intrinsic

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method
 *
 * Example:
 *
 *      throws(javaClass<IOException>())
 *      fun readFile(name: String): String {...}
 *
 * will be translated to
 *
 *      String readFile(String name) throws IOException {...}
 */
Retention(RetentionPolicy.SOURCE)
public annotation class throws(vararg val exceptionClasses: Class<out Throwable>)

[Intrinsic("kotlin.javaClass.property")] public val <T> T.javaClass : Class<T>
    get() = (this as java.lang.Object).getClass() as Class<T>

[Intrinsic("kotlin.javaClass.function")] fun <reified T> javaClass() : Class<T> = null as Class<T>


/**
 * A helper method for calling hashCode() on Kotlin objects
 * TODO remove when Any supports equals() and hashCode()
 */
[Intrinsic("kotlin.hashCode")] public fun Any.hashCode(): Int = (this as java.lang.Object).hashCode()

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
