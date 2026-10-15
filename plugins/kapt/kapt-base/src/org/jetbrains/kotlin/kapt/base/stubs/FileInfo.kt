/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.stubs

import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.tree.JCTree

fun JCTree.JCMethodDecl.getJavacSignature(): String {
    val name = name.toString()
    val params = parameters.joinToString { it.getType().withoutTypeAnnotations() }
    return "$name($params)"
}

// Annotations shall not be used in the lookup signatures, as they do not identify overloads and in
// some cases Javac can reorder them, e.g. move from type to parameter modifiers
private fun JCTree.withoutTypeAnnotations(): String = when (this) {
    is JCTree.JCAnnotatedType -> underlyingType.withoutTypeAnnotations()
    is JCTree.JCArrayTypeTree -> elemtype.withoutTypeAnnotations() + "[]"
    is JCTree.JCTypeApply -> clazz.withoutTypeAnnotations() + arguments.joinToString(
        prefix = "<", postfix = ">",
    ) { it.withoutTypeAnnotations() }
    is JCTree.JCFieldAccess -> selected.withoutTypeAnnotations() + "." + name
    is JCTree.JCWildcard -> when (kind.kind!!) {
        BoundKind.UNBOUND -> "?"
        BoundKind.EXTENDS -> "? extends " + inner.withoutTypeAnnotations()
        BoundKind.SUPER -> "? super " + inner.withoutTypeAnnotations()
    }
    else -> toString()
}

typealias LineInfoMap = MutableMap<String, KotlinPosition>

class FileInfo(private val lineInfo: LineInfoMap, private val signatureInfo: Map<String, String>) {
    companion object {
        val EMPTY = FileInfo(mutableMapOf(), emptyMap())
    }

    fun getPositionFor(fqName: String) = lineInfo[fqName]
    fun getMethodDescriptor(declaration: JCTree.JCMethodDecl) = signatureInfo[declaration.getJavacSignature()]
}
