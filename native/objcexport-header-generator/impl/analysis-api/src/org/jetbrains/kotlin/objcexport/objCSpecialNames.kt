package org.jetbrains.kotlin.objcexport

internal val objCSpecialNames = listOf("alloc", "copy", "mutableCopy", "new", "init")

internal fun String.handleSpecialNames(prefix: String): String {
    return if (this.isASpecialName()) {
        prefix + this.replaceFirstChar(Char::uppercaseChar)
    } else {
        this
    }
}

internal fun String.isASpecialName(): Boolean {
    val trimmed = this.dropWhile { it == '_' }
    return objCSpecialNames.any { trimmed.startsWithWords(it) }
}

internal fun String.isSpecialFamilyOrInit(explicitMethodFamilyName: Boolean): Boolean {
    // We're always interested in names that are/start with "init", irrespective of explicitMethodFamilyName.
    return !explicitMethodFamilyName && this.isASpecialName() || this == "init" || this.startsWith("initWith")
}
