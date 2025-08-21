// from https://github.com/node-gradle/gradle-node-plugin
package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.provider.Provider
import java.io.File

internal data class Platform(
    val name: String,
    val arch: String,
) {
    fun isWindows(): Boolean {
        return name == "win"
    }
}

internal enum class OsType(val osName: String) {
    WINDOWS("win"),
    MAC("darwin"),
    LINUX("linux"),
    FREEBSD("linux"), // https://github.com/node-gradle/gradle-node-plugin/issues/178
}

internal fun parsePlatform(name: String, arch: String, uname: Provider<String>): Platform {
    val osType = parseOsType(name)
    val osArch = if (osType == OsType.WINDOWS) parseWindowsArch(arch.lowercase(), uname)
    else parseOsArch(arch.lowercase(), uname)

    return Platform(
        osType.osName,
        osArch
    )
}

internal fun parseOsType(type: String): OsType {
    val name = type.lowercase()
    return when {
        name.contains("windows") -> OsType.WINDOWS
        name.contains("mac") -> OsType.MAC
        name.contains("linux") -> OsType.LINUX
        name.contains("freebsd") -> OsType.FREEBSD
        else -> error("Unsupported OS: $name")
    }
}

internal fun parseOsArch(arch: String, uname: Provider<String>): String {
    return when {
        /*
         * As Java just returns "arm" on all ARM variants, we need a system call to determine the exact arch. Unfortunately some JVMs say aarch32/64, so we need an additional
         * conditional. Additionally, the node binaries for 'armv8l' are called 'arm64', so we need to distinguish here.
         */
        arch == "arm" || arch.startsWith("aarch") -> uname.get()
            .let {
                if (it == "armv8l" || it == "aarch64") {
                    "arm64"
                } else it
            }
            .let {
                if (it == "x86_64") {
                    "x64"
                } else it
            }
        arch == "ppc64le" -> "ppc64le"
        arch == "s390x" -> "s390x"
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

internal fun parseWindowsArch(arch: String, uname: Provider<String>): String {
    return when {
        arch.startsWith("aarch") || arch.startsWith("arm")
            -> {
            val wmiArch = uname.get()
            return when (wmiArch) {
                /*
                 * Parse Win32_Processor.Architectures to real processor type
                 *
                 * Table from https://learn.microsoft.com/en-us/windows/win32/api/sysinfoapi/ns-sysinfoapi-system_info#members
                 */
                "12" -> "arm64"
                "9" -> "x64"
                // "6" -> "IA64"
                // "5" -> "arm" // 32-bit
                "0" -> "x86"
                // "0xffff" -> "Unknown"
                else -> error("Unexpected Win32_Processor.Architecture: $arch")
            }
        }
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

internal fun computeNpmScriptFile(
    nodeDirProvider: File,
    command: String,
    isWindows: Boolean
): String {
    return nodeDirProvider.let { nodeDir ->
        if (isWindows) nodeDir.resolve("node_modules/npm/bin/$command-cli.js").path
        else nodeDir.resolve("lib/node_modules/npm/bin/$command-cli.js").path
    }
}

internal fun computeNodeBinDir(
    nodeDirProvider: File,
    isWindows: Boolean
) =
    if (isWindows) nodeDirProvider else nodeDirProvider.resolve("bin")