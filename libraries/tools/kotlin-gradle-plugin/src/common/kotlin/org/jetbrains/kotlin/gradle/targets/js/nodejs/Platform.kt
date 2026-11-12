// from https://github.com/node-gradle/gradle-node-plugin
package org.jetbrains.kotlin.gradle.targets.js.nodejs

import java.nio.file.Path

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

internal fun parsePlatform(osType: OsType, arch: String): Platform {
    val osArch = if (osType == OsType.WINDOWS) parseWindowsArch(arch.lowercase())
    else parseOsArch(arch.lowercase())

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

internal fun parseOsArch(arch: String): String {
    return when {
        // Different JVMs name 64-bit ARM "aarch64" or "arm64".
        arch == "aarch64" || arch == "arm64" -> "arm64"
        // Java reports just "arm" (or "aarch32") for every 32-bit ARM variant and we no longer exec `uname` to tell
        // them apart. Node.js ships a single 32-bit ARM distribution, "armv7l", so we always use it. This is also
        // correct for 32-bit userland on an armv8l kernel (previously mapped to "arm64", which cannot run there).
        // armv6l is not distinguished: nodejs.org publishes no official armv6l builds.
        arch == "arm" || arch == "aarch32" -> "armv7l"
        arch == "ppc64le" -> "ppc64le"
        arch == "s390x" -> "s390x"
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

internal fun parseWindowsArch(arch: String): String {
    return when {
        // Node.js publishes no 32-bit ARM Windows build, so any ARM JVM is mapped to arm64.
        arch.startsWith("aarch") || arch.startsWith("arm") -> "arm64"
        arch.contains("64") -> "x64"
        else -> "x86"
    }
}

internal fun computeNpmScriptFile(
    nodeDir: Path,
    command: String,
    isWindows: Boolean,
): Path {
    return if (isWindows) {
        nodeDir.resolve("node_modules/npm/bin/$command-cli.js")
    } else {
        nodeDir.resolve("lib/node_modules/npm/bin/$command-cli.js")
    }
}

internal fun computeNodeBinDir(
    nodeDir: Path,
    isWindows: Boolean,
): Path =
    if (isWindows) nodeDir else nodeDir.resolve("bin")
