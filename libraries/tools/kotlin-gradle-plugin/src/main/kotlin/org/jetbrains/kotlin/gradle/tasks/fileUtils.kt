package org.jetbrains.kotlin.gradle.tasks

import java.io.File

internal fun File.isJavaFile() =
        extension.equals("java", ignoreCase = true)

internal fun File.isKotlinFile(): Boolean =
    extension.let {
        "kt".equals(it, ignoreCase = true) ||
        "kts".equals(it, ignoreCase = true)
    }

internal fun File.isClassFile(): Boolean =
        extension.equals("class", ignoreCase = true)

internal fun listClassFiles(path: String): Sequence<File> =
        File(path).walk().filter { it.isFile && it.isClassFile() }

