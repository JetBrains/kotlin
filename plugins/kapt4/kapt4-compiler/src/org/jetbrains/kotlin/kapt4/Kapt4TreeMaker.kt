/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.Disposable
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.sun.tools.javac.code.BoundKind
import com.sun.tools.javac.code.TypeTag
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import com.sun.tools.javac.util.Names
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.name.FqName

class Kapt4TreeMaker(
    context: Context,
    kaptContext: Kapt4ContextForStubGeneration
) : TreeMaker(context), Disposable {
    private var kaptContext = DisposableReference(kaptContext)

    val nameTable: Name.Table = Names.instance(context).table

    @Suppress("FunctionName")
    fun RawType(type: Type): JCTree.JCExpression {
        convertBuiltinType(type)?.let { return it }
        if (type.sort == ARRAY) {
            return TypeArray(RawType(AsmUtil.correctElementType(type)))
        }
        return FqName(type.internalName)
    }

    // TODO
    @Suppress("FunctionName")
    fun RawType(type: PsiType?): JCTree.JCExpression {
//        convertBuiltinType(type)?.let { return it }
//        if (type.sort == ARRAY) {
//            return TypeArray(Type(AsmUtil.correctElementType(type)))
//        } )
        return when (type) {
            is PsiArrayType -> TypeArray(RawType(type.componentType))
            is PsiWildcardType -> Wildcard(TypeBoundKind(BoundKind.UNBOUND), null)
            null -> TODO()
            else -> FqName(type.qualifiedName)
        }
    }

    @Suppress("FunctionName")
    fun TypeWithArguments(type: PsiType?): JCTree.JCExpression {
        return when (type) {
            is PsiArrayType -> TypeArray(TypeWithArguments(type.componentType))
            is PsiClassType -> {
                val rawBase = RawType(type)
                val arguments = type.parameters.takeIf { it.isNotEmpty() } ?: return rawBase
                val jArguments = mapJList(arguments) { TypeWithArguments(it) }

                return TypeApply(rawBase, jArguments)
            }

            is PsiWildcardType -> {
                val argumentType = type.bound?.let { TypeWithArguments(it) }
                when {
                    type.isExtends -> Wildcard(TypeBoundKind(BoundKind.EXTENDS), argumentType)
                    type.isSuper -> Wildcard(TypeBoundKind(BoundKind.SUPER), argumentType)
                    else -> Wildcard(TypeBoundKind(BoundKind.UNBOUND), argumentType)
                }
            }

            else -> RawType(type)
        }
    }

    @Suppress("FunctionName")
    fun FqName(internalOrFqName: String): JCTree.JCExpression {
        val path = getQualifiedName(internalOrFqName).convertSpecialFqName().split('.')
        assert(path.isNotEmpty())
        return FqName(path)
    }

    @Suppress("FunctionName")
    fun FqName(fqName: FqName): JCTree.JCExpression {
        return FqName(fqName.pathSegments().map { it.asString() })
    }

    @Suppress("FunctionName")
    private fun FqName(path: List<String>): JCTree.JCExpression {
        if (path.size == 1) return SimpleName(path.single())

        var expr = Select(SimpleName(path[0]), name(path[1]))
        for (index in 2..path.lastIndex) {
            expr = Select(expr, name(path[index]))
        }
        return expr
    }

    fun getQualifiedName(type: PsiType): String {
        if (type !is PsiClassType) TODO()
        val klass = type.resolve() ?: TODO()
        return getQualifiedName(klass)
    }

    fun getQualifiedName(type: PsiClass): String = getQualifiedName(type.qualifiedName!!)

    fun getSimpleName(clazz: PsiClass): String = clazz.name!!

    fun getQualifiedName(internalName: String): String {
        val nameWithDots = internalName.replace('/', '.')
        // This is a top-level class
        if ('$' !in nameWithDots) return nameWithDots

        return nameWithDots.replace('$', '.')
//        TODO()
//        // Maybe it's in our sources?
//        val classFromSources = kaptContext.compiledClasses.firstOrNull { it.name == internalName }
//        if (classFromSources != null) {
//            // Get inner class node pointing to the outer class
//            val innerClassNode = classFromSources.innerClasses.firstOrNull { it.name == classFromSources.name }
//            return innerClassNode?.let { getQualifiedName(it.outerName) + "." + it.innerName } ?: nameWithDots
//        }
//
//        // Search in the classpath
//        val javaPsiFacade = JavaPsiFacade.getInstance(kaptContext.project)
//        val scope = GlobalSearchScope.allScope(javaPsiFacade.project)
//
//        val fqNameFromClassWithPreciseName = javaPsiFacade.findClass(nameWithDots, scope)?.qualifiedName
//        if (fqNameFromClassWithPreciseName != null) {
//            return fqNameFromClassWithPreciseName
//        }
//
//        nameWithDots.iterateDollars { outerName, innerName ->
//            if (innerName.isEmpty()) return@iterateDollars // We already checked an exact match
//
//            val outerClass = javaPsiFacade.findClass(outerName, scope) ?: return@iterateDollars
//            return tryToFindNestedClass(outerClass, innerName)?.qualifiedName ?: return@iterateDollars
//        }
//
//        return nameWithDots
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
        var dollarIndex = this.indexOf('$', startIndex = 1)

        while (dollarIndex > 0) {
            val previousSymbol = this[dollarIndex - 1]
            val nextSymbol = this.getOrNull(dollarIndex + 1)

            if (previousSymbol != '.' && nextSymbol != '.') {
                val outerName = this.take(dollarIndex)
                val innerName = this.drop(dollarIndex + 1)

                if (outerName.isNotEmpty() && innerName.isNotEmpty()) {
                    variantHandler(outerName, innerName)
                }
            }

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

    @Suppress("FunctionName")
    fun SimpleName(name: String): JCTree.JCExpression = Ident(name(name))

    fun name(name: String): Name = nameTable.fromString(name)

    override fun dispose() {
        kaptContext.dispose()
    }

    companion object {
        internal fun preRegister(context: Context, kaptContext: Kapt4ContextForStubGeneration) {
            context.put(treeMakerKey, Context.Factory { Kapt4TreeMaker(it, kaptContext) })
        }
    }
}


private class DisposableReference<T : Any>(obj: T) : Disposable {
    private var obj: T? = obj
    fun get() = obj!!

    override fun dispose() {
        obj = null
    }
}
