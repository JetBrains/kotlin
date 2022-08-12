/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.JCTree
import java.io.File
import java.net.URI
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.tools.JavaFileObject

class KaptJavaFileObject(
    val compilationUnit: JCTree.JCCompilationUnit,
    val clazz: JCTree.JCClassDecl,
    val file: File? = null,
    val timestamp: Long = System.currentTimeMillis()
) : JavaFileObject {
    override fun toString() = "${javaClass.simpleName}[$name]"

    override fun isNameCompatible(simpleName: String?, kind: JavaFileObject.Kind?): Boolean {
        if (simpleName == null || kind == null) return false
        return this.kind == kind && simpleName == clazz.simpleName.toString()
    }

    override fun getKind() = JavaFileObject.Kind.SOURCE

    override fun getName(): String {
        val packageName = compilationUnit.getPackageNameJava9Aware()
        if (packageName == null || packageName.toString() == "") {
            return clazz.name.toString() + ".java"
        }
        return packageName.toString().replace('.', '/') + '/' + clazz.simpleName.toString() + ".java"
    }

    override fun getAccessLevel(): Modifier? {
        val flags = clazz.modifiers.getFlags()
        if (Modifier.PUBLIC in flags) return Modifier.PUBLIC
        if (Modifier.PROTECTED in flags) return Modifier.PROTECTED
        if (Modifier.PRIVATE in flags) return Modifier.PRIVATE
        return null
    }

    override fun openInputStream() = getCharContent(false).byteInputStream()

    override fun getCharContent(ignoreEncodingErrors: Boolean) = compilationUnit.toString()

    override fun getNestingKind() = NestingKind.TOP_LEVEL

    override fun toUri(): URI? = file?.toURI()

    override fun openReader(ignoreEncodingErrors: Boolean) = getCharContent(ignoreEncodingErrors).reader()

    override fun openWriter() = throw UnsupportedOperationException()

    override fun getLastModified() = timestamp

    override fun openOutputStream() = throw UnsupportedOperationException()

    override fun delete() = throw UnsupportedOperationException()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        other as KaptJavaFileObject

        if (compilationUnit != other.compilationUnit) return false
        if (clazz != other.clazz) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + compilationUnit.hashCode()
        result = 31 * result + clazz.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}
