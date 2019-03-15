/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.stubs

import com.sun.tools.javac.tree.JCTree

fun JCTree.JCMethodDecl.getJavacSignature(): String {
    val name = name.toString()
    val params = parameters.joinToString { it.getType().toString() }
    return "$name($params)"
}

typealias LineInfoMap = MutableMap<String, KotlinPosition>

class FileInfo(private val lineInfo: LineInfoMap, private val signatureInfo: Map<String, String>) {
    companion object {
        val EMPTY = FileInfo(mutableMapOf(), emptyMap())
    }

    fun getPositionFor(fqName: String) = lineInfo[fqName]
    fun getMethodDescriptor(declaration: JCTree.JCMethodDecl) = signatureInfo[declaration.getJavacSignature()]
}