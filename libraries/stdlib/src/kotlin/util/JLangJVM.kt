package kotlin

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.jvm.internal.unsafe.*
import kotlin.jvm.internal.Intrinsic

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.
 *
 * Example:
 *
 * ```
 * throws(javaClass<IOException>())
 * fun readFile(name: String): String {...}
 * ```
 *
 * will be translated to
 *
 * ```
 * String readFile(String name) throws IOException {...}
 * ```
 *
 * @property exceptionClasses the list of checked exception classes that may be thrown by the function.
 */
Retention(RetentionPolicy.SOURCE)
public annotation class throws(public vararg val exceptionClasses: Class<out Throwable>)

/**
 * Returns the runtime Java class of this object.
 */
[Intrinsic("kotlin.javaClass.property")] public val <T: Any> T.javaClass : Class<T>
    get() = (this as java.lang.Object).getClass() as Class<T>

/**
 * Returns the Java class for the specified type.
 */
[Intrinsic("kotlin.javaClass.function")] public fun <reified T: Any> javaClass(): Class<T> = null as Class<T>

/**
 * Executes the given function [block] while holding the monitor of the given object [lock].
 */
public inline fun <R> synchronized(lock: Any, block: () -> R): R {
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        monitorExit(lock)
    }
}

/**
 * Returns the annotation type of this annotation.
 */
public fun <T : Annotation> T.annotationType() : Class<out T> =
    (this as java.lang.annotation.Annotation).annotationType() as Class<out T>
