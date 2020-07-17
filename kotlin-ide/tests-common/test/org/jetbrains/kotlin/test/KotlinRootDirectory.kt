package org.jetbrains.kotlin.test

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object KotlinRoot {
    @JvmField
    val PATH: String = run {
        var current = Paths.get(".").toAbsolutePath().normalize()
        while (current != null && !Files.isRegularFile(current.resolve("kotlin.kotlin-ide.iml"))) {
            current = current.parent
        }
        checkNotNull(current) { "Cannot find kotiln-ide root" }
        current = current.resolve("kotlin")
        return@run current.toString()
    }

    @JvmField
    val DIR: File = File(PATH)
}