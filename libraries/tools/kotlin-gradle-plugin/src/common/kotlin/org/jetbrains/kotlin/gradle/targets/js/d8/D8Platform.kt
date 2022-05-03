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

    const val X64 = "64"
    const val X86 = "86"

    val architecture: String
        get() = if (property("os.arch").contains("64")) X64 else X86
}
