package org.jetbrains.kotlin.test

import java.io.File
import java.nio.file.Path

object KotlinRoot {
    @JvmField
    val DIR: File = run {
        var current = File(".").canonicalFile
        while (!current.isRepositoryRootDirectory()) {
            current = current.parentFile
        }
        checkNotNull(current) { "Can't find kotlin-ide root" }
        return@run current.resolve("kotlin")
    }

    @JvmField
    val PATH: Path = DIR.toPath()
}

private fun File.isRepositoryRootDirectory(): Boolean {
    if (resolve("kotlin.kotlin-ide.iml").isFile && resolve("intellij").isDirectory && resolve("kotlin/idea/kotlin.idea.iml").isFile) {
        // Ultimate repository
        return true
    }

    if (resolve("kotlin.intellij-kotlin.iml").isFile && resolve("intellij").isDirectory && resolve("idea/kotlin.idea.iml").isFile) {
        // Community (intellij-kotlin) repository
        return true
    }

    return false
}