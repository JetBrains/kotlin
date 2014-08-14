package kotlin

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.jvm.internal.unsafe.*
import kotlin.jvm.internal.Intrinsic
import kotlin.InlineOption.ONLY_LOCAL_RETURN

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
public annotation class throws(public vararg val exceptionClasses: Class<out Throwable>)

[Intrinsic("kotlin.javaClass.property")] public val <T: Any> T.javaClass : Class<T>
    get() = (this as java.lang.Object).getClass() as Class<T>

[Intrinsic("kotlin.javaClass.function")] public fun <reified T: Any> javaClass(): Class<T> = null as Class<T>

public inline fun <R> synchronized(lock: Any, [inlineOptions(ONLY_LOCAL_RETURN)] block: () -> R): R {
    monitorEnter(lock)
    try {
        return block()
    }
    finally {
        monitorExit(lock)
    }
}

public fun <T : Annotation> T.annotationType() : Class<out T> =
    (this as java.lang.annotation.Annotation).annotationType() as Class<out T>
