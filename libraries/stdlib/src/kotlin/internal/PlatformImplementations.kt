@file:JvmVersion
package kotlin.internal

import java.io.Closeable
import java.util.regex.MatchResult

internal open class PlatformImplementations {

    public open fun closeSuppressed(instance: Closeable, cause: Throwable) {
        try {
            instance.close()
        } catch (closeException: Throwable) {
            // eat the closeException as we are already throwing the original cause
            // and we don't want to mask the real exception;
            // on Java 7 we should call
            // e.addSuppressed(closeException)
        }
    }

    public open fun getMatchResultNamedGroup(matchResult: MatchResult, name: String): MatchGroup? {
        throw UnsupportedOperationException("Retrieving groups by name is not supported on this platform.")
    }

    companion object {
        @JvmField
        @InlineExposed
        val INSTANCE: PlatformImplementations = run {
            val version = getJavaVersion()
            try {
                if (version >= 0x10008)
                    return@run Class.forName("kotlin.internal.JDK8PlatformImplementations").newInstance() as PlatformImplementations
            } catch (e: ClassNotFoundException) {}

            try {
                if (version >= 0x10007)
                    return@run Class.forName("kotlin.internal.JDK7PlatformImplementations").newInstance() as PlatformImplementations
            } catch (e: ClassNotFoundException) {}

            PlatformImplementations()
        }

        private fun getJavaVersion(): Int {
            val default = 0x10006
            val version = System.getProperty("java.version") ?: return default
            val firstDot = version.indexOf('.')
            if (firstDot < 0) return default
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

    }


}

