@file:JvmVersion
package kotlin.internal

import java.io.Closeable
import java.util.regex.MatchResult

internal open class PlatformImplementations {

    public open fun addSuppressed(cause: Throwable, exception: Throwable) {
        // do nothing
    }

    public open fun getMatchResultNamedGroup(matchResult: MatchResult, name: String): MatchGroup? {
        throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")
    }
}

// TODO: drop before 1.1
@SinceKotlin("1.1")
@PublishedApi
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
internal fun platformCloseSuppressed(instance: Closeable, cause: Throwable) = instance.closeFinally(cause)


@JvmField
internal val IMPLEMENTATIONS: PlatformImplementations = run {
    val version = getJavaVersion()
    try {
        if (version >= 0x10008)
            return@run Class.forName("kotlin.internal.JRE8PlatformImplementations").newInstance() as PlatformImplementations
    } catch (e: ClassNotFoundException) {}

    try {
        if (version >= 0x10007)
            return@run Class.forName("kotlin.internal.JRE7PlatformImplementations").newInstance() as PlatformImplementations
    } catch (e: ClassNotFoundException) {}

    PlatformImplementations()
}

private fun getJavaVersion(): Int {
    val default = 0x10006
    val version = System.getProperty("java.specification.version") ?: return default
    val firstDot = version.indexOf('.')
    if (firstDot < 0)
        return try { version.toInt() * 0x10000 } catch (e: NumberFormatException) { default }

    var secondDot = version.indexOf('.', firstDot + 1)
    if (secondDot < 0) secondDot = version.length

    val firstPart = version.substring(0, firstDot)
    val secondPart = version.substring(firstDot + 1, secondDot)
    return try {
        firstPart.toInt() * 0x10000 + secondPart.toInt()
    } catch (e: NumberFormatException) {
        default
    }
}

