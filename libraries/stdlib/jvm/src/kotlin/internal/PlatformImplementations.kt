@file:JvmVersion
package kotlin.internal

import kotlin.*
import java.util.regex.MatchResult

internal open class PlatformImplementations {

    public open fun addSuppressed(cause: Throwable, exception: Throwable) {
        // do nothing
    }

    public open fun getMatchResultNamedGroup(matchResult: MatchResult, name: String): MatchGroup? {
        throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")
    }
}


@JvmField
internal val IMPLEMENTATIONS: PlatformImplementations = run {
    val version = getJavaVersion()
    if (version >= 0x10008) {
        try {
            return@run Class.forName("kotlin.internal.jdk8.JDK8PlatformImplementations").newInstance() as PlatformImplementations
        }
        catch (e: ClassNotFoundException) { }
        try {
            return@run Class.forName("kotlin.internal.JRE8PlatformImplementations").newInstance() as PlatformImplementations
        }
        catch (e: ClassNotFoundException) { }
    }

    if (version >= 0x10007) {
        try {
            return@run Class.forName("kotlin.internal.jdk7.JDK7PlatformImplementations").newInstance() as PlatformImplementations
        }
        catch (e: ClassNotFoundException) { }
        try {
            return@run Class.forName("kotlin.internal.JRE7PlatformImplementations").newInstance() as PlatformImplementations
        }
        catch (e: ClassNotFoundException) { }
    }

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

/**
 * Constant check of api version used during compilation
 *
 * This function is evaluated at compile time to a constant value,
 * so there should be no references to it in other modules.
 *
 * The function usages are validated to have literal argument values.
 */
@PublishedApi
@SinceKotlin("1.2")
internal fun apiVersionIsAtLeast(major: Int, minor: Int, patch: Int) =
        KotlinVersion.CURRENT.isAtLeast(major, minor, patch)