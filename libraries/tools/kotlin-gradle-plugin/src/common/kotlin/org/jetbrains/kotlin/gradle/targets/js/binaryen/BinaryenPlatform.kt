package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.provider.Provider

/**
 * Provides platform and architecture names that is used to download Binaryen.
 */
internal data class BinaryenPlatform(
    val name: String,
    val arch: String,
) {
    val platform: String
        get() = "$arch-$name"

    fun isWindows(): Boolean {
        return name == WIN
    }

    companion object {
        const val WIN = "windows"
        const val LINUX = "linux"
        const val DARWIN = "macos"

        const val X64 = "x86_64"
        const val X86 = "x86_86"

        internal fun parseBinaryenPlatform(name: String, arch: String, uname: Provider<String>): BinaryenPlatform {
            return BinaryenPlatform(
                parseOsName(name.toLowerCase()),
                parseOsArch(
                    arch.toLowerCase(),
                    uname
                )
            )
        }

        private fun parseOsName(name: String): String {
            return when {
                name.contains("windows") -> WIN
                name.contains("mac") -> DARWIN
                name.contains("linux") -> LINUX
                name.contains("freebsd") -> LINUX
                else -> throw IllegalArgumentException("Unsupported OS: $name")
            }
        }

        private fun parseOsArch(arch: String, uname: Provider<String>): String {
            return when {
                arch == "arm" || arch.startsWith("aarch") -> uname.get()
                arch.contains("64") -> X64
                else -> X86
            }
        }
    }
}