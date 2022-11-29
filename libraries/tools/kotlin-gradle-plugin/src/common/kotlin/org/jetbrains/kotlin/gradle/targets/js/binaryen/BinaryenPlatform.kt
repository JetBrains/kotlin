package org.jetbrains.kotlin.gradle.targets.js.binaryen

/**
 * Provides platform and architecture names that is used to download Binaryen.
 */
internal object BinaryenPlatform {
    private val props = System.getProperties()
    private fun property(name: String) = props.getProperty(name) ?: System.getProperty(name)

    const val WIN = "windows"
    const val LINUX = "linux"
    const val DARWIN = "macos"

    val name: String = run {
        val name = property("os.name").toLowerCase()
        when {
            name.contains("windows") -> WIN
            name.contains("mac") -> DARWIN
            name.contains("linux") -> LINUX
            name.contains("freebsd") -> LINUX
            else -> throw IllegalArgumentException("Unsupported OS: $name")
        }
    }

    const val ARM64 = "arm64"
    const val X64 = "x86_64"
    const val X86 = "x86_86"

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
        get() = "$architecture-$name"
}
