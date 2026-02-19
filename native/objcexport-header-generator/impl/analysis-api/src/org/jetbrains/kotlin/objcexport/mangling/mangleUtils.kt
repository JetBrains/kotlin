package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.cKeywords
import org.jetbrains.kotlin.objcexport.toValidObjCSwiftIdentifier

internal fun String.mangleSelector(postfix: String): String {
    return if (this.contains(":")) this.replace(":", "$postfix:")
    else this + postfix
}

internal fun unifyName(initialName: String, usedNames: Set<String>): String {
    var unique = initialName.toValidObjCSwiftIdentifier()
    while (unique in usedNames || unique in cKeywords) {
        unique += "_"
    }
    return unique
}