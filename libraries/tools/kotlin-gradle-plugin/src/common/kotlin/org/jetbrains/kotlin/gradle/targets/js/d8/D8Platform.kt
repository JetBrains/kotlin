package org.jetbrains.kotlin.gradle.targets.js.d8

/**
 * Provides platform and architecture names that is used to download D8.
 */
internal object D8Platform {
    private val props = System.getProperties()
    private fun property(name: String) = props.getProperty(name) ?: System.getProperty(name)

    const val WIN = "win"
    const val LINUX = "linux"
    const val DARWIN = "mac"

    val name: String
        get() {
            val osName = property("os.name").toLowerCase()
            return when {
                osName.contains("windows") -> WIN
                osName.contains("mac") -> DARWIN
                osName.contains("linux") -> LINUX
                osName.contains("freebsd") -> LINUX
                else -> throw IllegalArgumentException("Unsupported OS: $osName")
            }
        }

    const val ARM64 = "arm64"
    const val X64 = "64"
    const val X86 = "86"

    val architecture: String
        get() {
            val arch = property("os.arch")
            return when {
                arch == "aarch64" -> ARM64
                arch.contains("64") -> X64
                else -> X86
            }
        }

    val platform: String
        get() = when (val architecture = D8Platform.architecture) {
            ARM64 -> "$name-$ARM64"
            X64 -> name + X64
            X86 -> name + X86
            else -> error("Unexpected platform architecture $architecture")
        }
}