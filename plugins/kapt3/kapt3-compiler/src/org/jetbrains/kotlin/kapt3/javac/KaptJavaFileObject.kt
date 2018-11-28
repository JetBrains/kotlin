/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.javac

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
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