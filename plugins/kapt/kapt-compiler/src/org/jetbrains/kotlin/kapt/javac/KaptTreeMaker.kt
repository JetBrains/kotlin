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

package org.jetbrains.kotlin.kapt.javac

import com.intellij.openapi.Disposable
import com.sun.tools.javac.code.TypeTag
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import com.sun.tools.javac.util.Names
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class KaptTreeMaker(context: Context, kaptContext: KaptContextForStubGeneration) : TreeMaker(context), Disposable {
    data class ClassInfo(val name: String, val isInner: Boolean)

    data class ResolvedQualifiedName(
        val packageName: String,
        /** Classes from the outermost to the innermost class. Literal '$' characters in names are preserved. */
        val classInfo: List<ClassInfo>,
    ) {
        val qualifiedName: String =
            (if (packageName.isEmpty()) "" else "$packageName.") + classInfo.joinToString(".") { it.name }
    }

    private var kaptContext = DisposableReference(kaptContext)

    val nameTable: Name.Table = Names.instance(context).table

    private val resolvedQualifiedNameCache = HashMap<String, ResolvedQualifiedName>()

    @Suppress("FunctionName")
    fun Type(type: Type): JCTree.JCExpression {
        convertBuiltinType(type)?.let { return it }
        if (type.sort == ARRAY) {
            return TypeArray(Type(AsmUtil.correctElementType(type)))
        }
        return FqName(type.internalName)
    }

    @Suppress("FunctionName")
    fun FqName(internalOrFqName: String): JCTree.JCExpression {
        val path = getQualifiedName(internalOrFqName).split('.')
        assert(path.isNotEmpty())
        return FqName(path)
    }

    @Suppress("FunctionName")
    fun FqName(fqName: FqName) = FqName(fqName.pathSegments().map { it.asString() })

    @Suppress("FunctionName")
    private fun FqName(path: List<String>): JCTree.JCExpression {
        if (path.size == 1) return SimpleName(path.single())

        var expr = Select(SimpleName(path[0]), name(path[1]))
        for (index in 2..path.lastIndex) {
            expr = Select(expr, name(path[index]))
        }
        return expr
    }

    fun getQualifiedName(type: Type) = getQualifiedName(type.internalName)

    fun convertAsmTypeToJavaText(type: Type): String = when (type.sort) {
        VOID -> "void"
        BOOLEAN -> "boolean"
        CHAR -> "char"
        BYTE -> "byte"
        SHORT -> "short"
        INT -> "int"
        FLOAT -> "float"
        LONG -> "long"
        DOUBLE -> "double"
        ARRAY -> convertAsmTypeToJavaText(type.elementType) + "[]".repeat(type.dimensions)
        OBJECT -> getQualifiedName(type)
        else -> kaptContext.get().textGenerationError("Unsupported ASM type: $type")
    }

    fun getSimpleName(clazz: ClassNode) = getQualifiedName(clazz.name).substringAfterLast('.')

    fun getQualifiedName(internalName: String): String = resolveQualifiedName(internalName).qualifiedName

    /**
     * Resolves a JVM internal name and identifies which '$' characters separate nested classes.
     * [ResolvedQualifiedName.classInfo] retains '$' characters that are part of a class name.
     */
    fun resolveQualifiedName(internalName: String): ResolvedQualifiedName =
        resolvedQualifiedNameCache.getOrPut(internalName) { computeQualifiedName(internalName) }

    private fun computeQualifiedName(internalName: String): ResolvedQualifiedName {
        val packageName = internalName.substringBeforeLast('/', missingDelimiterValue = "").replace('/', '.')
        fun topLevelName() = ResolvedQualifiedName(
            packageName,
            listOf(ClassInfo(internalName.substringAfterLast('/'), isInner = false)),
        )
        // No nested-class separators to resolve.
        if ('$' !in internalName) return topLevelName()

        val kaptContext = this.kaptContext.get()

        // Maybe it's in our sources?
        val classFromSources = kaptContext.compiledClassByName[internalName]
        if (classFromSources != null) {
            // Get inner class node pointing to the outer class
            val innerClassNode = classFromSources.innerClasses.firstOrNull { it.name == classFromSources.name }
            return innerClassNode?.let {
                val outerName = resolveQualifiedName(it.outerName)
                ResolvedQualifiedName(
                    outerName.packageName,
                    outerName.classInfo + ClassInfo(it.innerName, isInner = it.access and Opcodes.ACC_STATIC == 0),
                )
            } ?: topLevelName()
        }

        // Search in the classpath
        val firSession = kaptContext.firSession!!
        val nameWithDots = internalName.replace('/', '.')
        val topLevelClassId = ClassId.topLevel(org.jetbrains.kotlin.name.FqName(nameWithDots))
        if (firSession.getRegularClassSymbolByClassId(topLevelClassId) != null) {
            return topLevelName()
        }

        nameWithDots.iterateDollars { outerName, innerName ->
            if (innerName.isEmpty()) return@iterateDollars // We already checked an exact match

            val classId = ClassId.topLevel(org.jetbrains.kotlin.name.FqName(outerName))
            val outerClass = firSession.getRegularClassSymbolByClassId(classId) ?: return@iterateDollars
            with(firSession) {
                val nestedClasses = tryToFindNestedClass(outerClass, innerName) ?: return@iterateDollars
                return ResolvedQualifiedName(
                    outerClass.classId.packageFqName.asString(),
                    (listOf(outerClass) + nestedClasses).map { ClassInfo(it.classId.shortClassName.asString(), it.isInner) },
                )
            }
        }

        // If FIR lookups fail, preserve existing dot-separated components and leave '$' characters literal.
        return ResolvedQualifiedName(
            packageName,
            internalName.substringAfterLast('/').split('.').map { ClassInfo(it, isInner = false) },
        )
    }

    context(session: FirSession)
    private fun tryToFindNestedClass(outerClass: FirRegularClassSymbol, innerClassName: String): List<FirRegularClassSymbol>? {
        outerClass.findInnerClassByName(innerClassName)?.let { return listOf(it) }

        innerClassName.iterateDollars { name1, name2 ->
            if (name2.isEmpty()) return outerClass.findInnerClassByName(name1)?.let { listOf(it) }

            val nestedClass = outerClass.findInnerClassByName(name1)
            if (nestedClass != null) {
                tryToFindNestedClass(nestedClass, name2)?.let { return listOf(nestedClass) + it }
            }
        }

        return null
    }

    context(session: FirSession)
    private fun FirRegularClassSymbol.findInnerClassByName(name: String): FirRegularClassSymbol? =
        session.getRegularClassSymbolByClassId(classId.createNestedClassId(org.jetbrains.kotlin.name.Name.identifier(name)))

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
        resolvedQualifiedNameCache.clear()
        kaptContext.dispose()
    }

    companion object {
        internal fun preRegister(context: Context, kaptContext: KaptContextForStubGeneration) {
            context.put(treeMakerKey) { KaptTreeMaker(it, kaptContext) }
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
