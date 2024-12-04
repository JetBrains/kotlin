package org.jetbrains.kotlin.objcexport.mangling

internal fun String.mangleSelector(postfix: String): String {
    return if (this.contains(":")) this.replace(":", "$postfix:")
    else this + postfix
}