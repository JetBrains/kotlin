/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

import java.lang.reflect.Method
import java.lang.reflect.Modifier

private fun Class<*>.enclosingGenericOwner(): Class<*>? =
    if (isMemberClass && !Modifier.isStatic(modifiers)) enclosingClass else null

/**
 * Returns true if this is an inner class of a generic class, at any depth: a reference to it needs type arguments on
 * its outer part, which a dot-separated qualified name cannot carry.
 */
internal fun Class<*>.hasGenericOwner(): Boolean =
    generateSequence(enclosingGenericOwner()) { it.enclosingGenericOwner() }.any { it.typeParameters.isNotEmpty() }

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
    // The name of an array class is a descriptor (e.g. `[I`): its package is the one of its element class
    val elementClass = generateSequence(this) { it.componentType }.last()
    if (elementClass.isPrimitive) return true
    val packageName = elementClass.name.substringBeforeLast('.', missingDelimiterValue = "")
    return try {
        isExported.invoke(getModule.invoke(elementClass), packageName) as Boolean
    } catch (_: Exception) {
        true
    }
}
