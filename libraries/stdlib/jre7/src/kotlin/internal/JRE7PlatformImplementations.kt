package kotlin.internal


@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
internal open class JRE7PlatformImplementations : PlatformImplementations() {

    override fun addSuppressed(cause: Throwable, exception: Throwable) = cause.addSuppressed(exception)

}
