/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun Class<*>.enclosingGenericOwner(): Class<*>? =
    if (isMemberClass && !Modifier.isStatic(modifiers)) enclosingClass else null

/**
 * Returns false if a snippet can't reference this class: if this class or any class enclosing it is not public (in
 * the bytecode or, for Kotlin classes, in the Kotlin metadata, e.g. an `internal` class), or if its package is not
 * exported by its module.
 */
internal fun Class<*>.isAccessibleFromSnippets(): Boolean {
    if (!isExportedByItsModule()) return false
    var current: Class<*>? = this
    while (current != null) {
        if (!Modifier.isPublic(current.modifiers)) return false
        if (current.hasNonPublicKotlinVisibility()) return false
        current = current.enclosingClass
    }
    return true
}

// Accessed reflectively, since this module targets JDK 1.8, where modules don't exist.
private val classGetModuleMethod: Method? by lazy {
    try {
        Class::class.java.getMethod("getModule")
    } catch (_: NoSuchMethodException) {
        null
    }
}

private val moduleIsExportedMethod: Method? by lazy {
    try {
        classGetModuleMethod?.returnType?.getMethod("isExported", String::class.java)
    } catch (_: NoSuchMethodException) {
        null
    }
}

private fun Class<*>.isExportedByItsModule(): Boolean {
    val getModule = classGetModuleMethod ?: return true
    val isExported = moduleIsExportedMethod ?: return true
    val packageName = name.substringBeforeLast('.', missingDelimiterValue = "")
    return try {
        isExported.invoke(getModule.invoke(this), packageName) as Boolean
    } catch (_: Exception) {
        true
    }
}
