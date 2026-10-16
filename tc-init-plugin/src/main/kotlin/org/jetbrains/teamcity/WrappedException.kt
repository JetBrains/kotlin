package org.jetbrains.teamcity

class WrappedException(
    val className: String,
    override val message: String?,
    override val cause: Throwable?,
    stackTrace: Array<StackTraceElement>
) : RuntimeException(message, cause) {
    init {
        this.stackTrace = stackTrace
    }
}
