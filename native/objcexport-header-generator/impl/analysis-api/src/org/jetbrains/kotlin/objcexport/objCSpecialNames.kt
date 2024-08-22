package org.jetbrains.kotlin.objcexport

internal val objCSpecialNames = listOf("alloc", "copy", "mutableCopy", "new", "init")

internal fun String.handleSpecialNames(prefix: String): String {
    val trimmed = this.dropWhile { it == '_' }
    for (name in objCSpecialNames) {
        if (trimmed.startsWithWords(name)) return prefix + this.replaceFirstChar(Char::uppercaseChar)
    }
    return this
}