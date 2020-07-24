package org.jetbrains.kotlin.test

import java.io.File
import java.nio.file.Path

object KotlinRoot {
    @JvmField
    val DIR: File = run {
        var current = File(".").canonicalFile
        while (!current.resolve("kotlin.kotlin-ide.iml").isFile) {
            current = current.parentFile
        }
        checkNotNull(current) { "Can't find kotlin-ide root" }
        return@run current.resolve("kotlin")
    }

    @JvmField
    val PATH: Path = DIR.toPath()
}