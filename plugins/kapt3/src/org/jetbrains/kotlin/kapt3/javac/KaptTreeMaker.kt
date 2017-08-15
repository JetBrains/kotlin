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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import com.sun.tools.javac.util.Names
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class KaptTreeMaker(context: Context, private val kaptContext: KaptContext<*>) : TreeMaker(context) {
    val nameTable: Name.Table = Names.instance(context).table

    fun Type(type: Type): JCTree.JCExpression {
        convertBuiltinType(type)?.let { return it }
        if (type.sort == ARRAY) {
            return TypeArray(Type(AsmUtil.correctElementType(type)))
        }
        return FqName(type.internalName)
    }

    fun FqName(internalOrFqName: String): JCTree.JCExpression {
        val path = getQualifiedName(internalOrFqName).convertSpecialFqName().split('.')
        assert(path.isNotEmpty())
        if (path.size == 1) return SimpleName(path.single())

        var expr = Select(SimpleName(path[0]), name(path[1]))
        for (index in 2..path.lastIndex) {
            expr = Select(expr, name(path[index]))
        }
        return expr
    }

    fun getQualifiedName(type: Type) = getQualifiedName(type.internalName)

    fun getSimpleName(clazz: ClassNode) = getQualifiedName(clazz.name).substringAfterLast('.')

    fun getQualifiedName(internalName: String): String {
        val nameWithDots = internalName.replace('/', '.')
        // This is a top-level class
        if ('$' !in nameWithDots) return nameWithDots

        // Maybe it's in our sources?
        val classFromSources = kaptContext.compiledClasses.firstOrNull { it.name == internalName }
        if (classFromSources != null) {
            // Get inner class node pointing to the outer class
            val innerClassNode = classFromSources.innerClasses.firstOrNull { it.name == classFromSources.name }
            return innerClassNode?.let { getQualifiedName(it.outerName) + "." + it.innerName } ?: nameWithDots
        }

        // Search in the classpath
        val javaPsiFacade = JavaPsiFacade.getInstance(kaptContext.project)
        val scope = GlobalSearchScope.allScope(javaPsiFacade.project)

        val fqNameFromClassWithPreciseName = javaPsiFacade.findClass(nameWithDots, scope)?.qualifiedName
        if (fqNameFromClassWithPreciseName != null) {
            return fqNameFromClassWithPreciseName
        }

        nameWithDots.iterateDollars { outerName, innerName ->
            if (innerName.isEmpty()) return@iterateDollars // We already checked an exact match

            val outerClass = javaPsiFacade.findClass(outerName, scope) ?: return@iterateDollars
            return tryToFindNestedClass(outerClass, innerName)?.qualifiedName ?: return@iterateDollars
        }

        return nameWithDots
    }

    private fun tryToFindNestedClass(outerClass: PsiClass, innerClassName: String): PsiClass? {
        outerClass.findInnerClassByName(innerClassName, false)?.let { return it }

        innerClassName.iterateDollars { name1, name2 ->
            if (name2.isEmpty()) return outerClass.findInnerClassByName(name1, false)

            val nestedClass = outerClass.findInnerClassByName(name1, false)
            if (nestedClass != null) {
                tryToFindNestedClass(nestedClass, name2)?.let { return it }
            }
        }

        return null
    }

    private inline fun String.iterateDollars(variantHandler: (outerName: String, innerName: String) -> Unit) {
        var dollarIndex = this.indexOf('$')
        if (dollarIndex < 0) {
            variantHandler(this, "")
            return
        }

        while (dollarIndex > 0 && dollarIndex < this.lastIndex) {
            val outerName = this.take(dollarIndex)
            val innerName = this.drop(dollarIndex + 1)

            variantHandler(outerName, innerName)

            dollarIndex = this.indexOf('$', startIndex = dollarIndex + 1)
        }
    }

    private fun String.convertSpecialFqName(): String {
        // Hard-coded in ImplementationBodyCodegen, KOTLIN_MARKER_INTERFACES
        if (this == "kotlin.jvm.internal.markers.KMutableMap\$Entry") {
            return replace('$', '.')
        }

        return this
    }

    private fun convertBuiltinType(type: Type): JCTree.JCExpression? {
        val typeTag = when (type) {
            BYTE_TYPE -> TypeTag.BYTE
            BOOLEAN_TYPE -> TypeTag.BOOLEAN
            CHAR_TYPE -> TypeTag.CHAR
            SHORT_TYPE -> TypeTag.SHORT
            INT_TYPE -> TypeTag.INT
            LONG_TYPE -> TypeTag.LONG
            FLOAT_TYPE -> TypeTag.FLOAT
            DOUBLE_TYPE -> TypeTag.DOUBLE
            VOID_TYPE -> TypeTag.VOID
            else -> null
        } ?: return null
        return TypeIdent(typeTag)
    }

    fun SimpleName(name: String): JCTree.JCExpression = Ident(name(name))

    fun name(name: String): Name = nameTable.fromString(name)

    companion object {
        internal fun preRegister(context: Context, kaptContext: KaptContext<*>) {
            context.put(treeMakerKey, Context.Factory<TreeMaker> { KaptTreeMaker(it, kaptContext) })
        }
    }
}