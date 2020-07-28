package org.jetbrains.kotlin.test

import java.io.File

object KotlinRoot {
    @JvmField
    val REPO: File

    @JvmField
    val DIR: File

    init {
        var current = File(".").canonicalFile

        var kind = current.getKotlinRootKind()
        while (kind == null) {
            current = current.parentFile
            kind = current.getKotlinRootKind()
        }

        REPO = current

        DIR = when (kind) {
            KotlinRootKind.ULTIMATE -> current.resolve("kotlin")
            KotlinRootKind.COMMUNITY -> current
        }
    }
}

private enum class KotlinRootKind {
    ULTIMATE, COMMUNITY
}

private fun File.getKotlinRootKind(): KotlinRootKind? {
    if (resolve("kotlin.kotlin-ide.iml").isFile && resolve("intellij").isDirectory && resolve("kotlin/idea/kotlin.idea.iml").isFile) {
        return KotlinRootKind.ULTIMATE
    }

    if (resolve("kotlin.intellij-kotlin.iml").isFile && resolve("intellij").isDirectory && resolve("idea/kotlin.idea.iml").isFile) {
        return KotlinRootKind.COMMUNITY
    }

    return null
}