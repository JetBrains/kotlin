@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("ExceptionsKt")
@file:kotlin.jvm.JvmVersion
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "DeprecatedCallableAddReplaceWith")
package kotlin


/**
 * Returns an array of stack trace elements representing the stack trace
 * pertaining to this throwable.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public val Throwable.stackTrace: Array<StackTraceElement>
    get() = stackTrace!!
