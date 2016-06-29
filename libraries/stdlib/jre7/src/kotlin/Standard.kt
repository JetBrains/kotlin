@file:JvmName("StandardJRE7Kt")
package kotlin

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
public inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Throwable) {
        closed = true
        @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
        this?.closeSuppressed(e)
        throw e
    } finally {
        if (this != null && !closed) {
            close()
        }
    }
}

/**
 * Closes this [AutoCloseable] suppressing possible exception or error thrown by [AutoCloseable.close] function.
 * The suppressed exception is added to the list of suppressed exceptions of [cause] exception.
 */
@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineExposed
internal fun AutoCloseable.closeSuppressed(cause: Throwable) {
    try {
        close()
    } catch (closeException: Throwable) {
        cause.addSuppressed(closeException)
    }
}

