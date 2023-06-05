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

internal fun parsePlatform(name: String, arch: String, uname: Provider<String>): Platform {
    return Platform(
        parseOsName(name.toLowerCase()),
        parseOsArch(
            arch.toLowerCase(),
            uname
        )
    )
}

internal fun parseOsName(name: String): String {
    return when {
        name.contains("windows") -> "win"
        name.contains("mac") -> "darwin"
        name.contains("linux") -> "linux"
        name.contains("freebsd") -> "linux"
        name.contains("sunos") -> "sunos"
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