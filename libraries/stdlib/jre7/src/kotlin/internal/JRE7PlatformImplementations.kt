package kotlin.internal

import java.io.Closeable

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
internal open class JRE7PlatformImplementations : PlatformImplementations() {
    override fun closeSuppressed(instance: Closeable, cause: Throwable) = instance.closeSuppressed(cause)
}
