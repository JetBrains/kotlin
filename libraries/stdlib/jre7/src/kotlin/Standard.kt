@file:JvmName("StandardJRE7Kt")
package kotlin

// TODO: Drop before 1.1
@SinceKotlin("1.1")
@PublishedApi
@Deprecated("Provided for binary compatibility")
@JvmName("closeSuppressed")
internal fun AutoCloseable.closeSuppressedDeprecated(cause: Throwable) = closeSuppressed(cause)

