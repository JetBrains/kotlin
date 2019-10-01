/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.types

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun JKType.asTypeElement(annotationList: JKAnnotationList = JKAnnotationList()) =
    JKTypeElement(this, annotationList)

fun JKClassSymbol.asType(nullability: Nullability = Nullability.Default): JKClassType =
    JKClassType(this, emptyList(), nullability)

val PsiType.isKotlinFunctionalType: Boolean
    get() {
        val fqName = safeAs<PsiClassType>()?.resolve()?.getKotlinFqName() ?: return false
        return functionalTypeRegex.matches(fqName.asString())
    }

private val functionalTypeRegex = """(kotlin\.jvm\.functions|kotlin)\.Function[\d+]""".toRegex()

fun KtTypeReference.toJK(typeFactory: JKTypeFactory): JKType? =
    analyze(BodyResolveMode.PARTIAL)
        .get(BindingContext.TYPE, this)
        ?.let { typeFactory.fromKotlinType(it) }


infix fun JKJavaPrimitiveType.isStrongerThan(other: JKJavaPrimitiveType) =
    jvmPrimitiveTypesPriority.getValue(this.jvmPrimitiveType.primitiveType) >
            jvmPrimitiveTypesPriority.getValue(other.jvmPrimitiveType.primitiveType)

private val jvmPrimitiveTypesPriority =
    mapOf(
        PrimitiveType.BOOLEAN to -1,
        PrimitiveType.CHAR to 0,
        PrimitiveType.BYTE to 1,
        PrimitiveType.SHORT to 2,
        PrimitiveType.INT to 3,
        PrimitiveType.LONG to 4,
        PrimitiveType.FLOAT to 5,
        PrimitiveType.DOUBLE to 6
    )


fun JKType.applyRecursive(transform: (JKType) -> JKType?): JKType =
    transform(this) ?: when (this) {
        is JKTypeParameterType -> this
        is JKClassType ->
            JKClassType(
                classReference,
                parameters.map { it.applyRecursive(transform) },
                nullability
            )
        is JKNoType -> this
        is JKJavaVoidType -> this
        is JKJavaPrimitiveType -> this
        is JKJavaArrayType -> JKJavaArrayType(type.applyRecursive(transform), nullability)
        is JKContextType -> JKContextType
        is JKJavaDisjunctionType ->
            JKJavaDisjunctionType(disjunctions.map { it.applyRecursive(transform) }, nullability)
        is JKStarProjectionType -> this
        else -> this
    }

inline fun <reified T : JKType> T.updateNullability(newNullability: Nullability): T =
    if (nullability == newNullability) this
    else when (this) {
        is JKTypeParameterType -> JKTypeParameterType(identifier, newNullability)
        is JKClassType -> JKClassType(classReference, parameters, newNullability)
        is JKNoType -> this
        is JKJavaVoidType -> this
        is JKJavaPrimitiveType -> this
        is JKJavaArrayType -> JKJavaArrayType(type, newNullability)
        is JKContextType -> JKContextType
        is JKJavaDisjunctionType -> this
        else -> this
    } as T

@Suppress("UNCHECKED_CAST")
fun <T : JKType> T.updateNullabilityRecursively(newNullability: Nullability): T =
    applyRecursive {
        when (it) {
            is JKTypeParameterType -> JKTypeParameterType(it.identifier, newNullability)
            is JKClassType ->
                JKClassType(
                    it.classReference,
                    it.parameters.map { it.updateNullabilityRecursively(newNullability) },
                    newNullability
                )
            is JKJavaArrayType -> JKJavaArrayType(it.type.updateNullabilityRecursively(newNullability), newNullability)
            else -> null
        }
    } as T

fun JKType.isStringType(): Boolean =
    (this as? JKClassType)?.classReference?.isStringType() == true

fun JKClassSymbol.isStringType(): Boolean =
    fqName == CommonClassNames.JAVA_LANG_STRING
            || fqName == KotlinBuiltIns.FQ_NAMES.string.asString()

fun JKJavaPrimitiveType.toLiteralType(): JKLiteralExpression.LiteralType? =
    when (this) {
        JKJavaPrimitiveType.CHAR -> JKLiteralExpression.LiteralType.CHAR
        JKJavaPrimitiveType.BOOLEAN -> JKLiteralExpression.LiteralType.BOOLEAN
        JKJavaPrimitiveType.INT -> JKLiteralExpression.LiteralType.INT
        JKJavaPrimitiveType.LONG -> JKLiteralExpression.LiteralType.LONG
        JKJavaPrimitiveType.CHAR -> JKLiteralExpression.LiteralType.CHAR
        JKJavaPrimitiveType.DOUBLE -> JKLiteralExpression.LiteralType.DOUBLE
        JKJavaPrimitiveType.FLOAT -> JKLiteralExpression.LiteralType.FLOAT
        else -> null
    }


fun JKType.asPrimitiveType(): JKJavaPrimitiveType? =
    if (this is JKJavaPrimitiveType) this
    else when ((this as? JKClassType)?.classReference?.fqName) {
        KotlinBuiltIns.FQ_NAMES._char.asString(), CommonClassNames.JAVA_LANG_CHARACTER -> JKJavaPrimitiveType.CHAR
        KotlinBuiltIns.FQ_NAMES._boolean.asString(), CommonClassNames.JAVA_LANG_BOOLEAN -> JKJavaPrimitiveType.BOOLEAN
        KotlinBuiltIns.FQ_NAMES._int.asString(), CommonClassNames.JAVA_LANG_INTEGER -> JKJavaPrimitiveType.INT
        KotlinBuiltIns.FQ_NAMES._long.asString(), CommonClassNames.JAVA_LANG_LONG -> JKJavaPrimitiveType.LONG
        KotlinBuiltIns.FQ_NAMES._float.asString(), CommonClassNames.JAVA_LANG_FLOAT -> JKJavaPrimitiveType.FLOAT
        KotlinBuiltIns.FQ_NAMES._double.asString(), CommonClassNames.JAVA_LANG_DOUBLE -> JKJavaPrimitiveType.DOUBLE
        KotlinBuiltIns.FQ_NAMES._byte.asString(), CommonClassNames.JAVA_LANG_BYTE -> JKJavaPrimitiveType.BYTE
        KotlinBuiltIns.FQ_NAMES._short.asString(), CommonClassNames.JAVA_LANG_SHORT -> JKJavaPrimitiveType.SHORT
        else -> null
    }

fun JKJavaPrimitiveType.isNumberType() =
    this == JKJavaPrimitiveType.INT ||
            this == JKJavaPrimitiveType.LONG ||
            this == JKJavaPrimitiveType.FLOAT ||
            this == JKJavaPrimitiveType.DOUBLE


val primitiveTypes =
    listOf(
        JvmPrimitiveType.BOOLEAN,
        JvmPrimitiveType.CHAR,
        JvmPrimitiveType.BYTE,
        JvmPrimitiveType.SHORT,
        JvmPrimitiveType.INT,
        JvmPrimitiveType.FLOAT,
        JvmPrimitiveType.LONG,
        JvmPrimitiveType.DOUBLE
    )

fun JKType.arrayFqName(): String =
    if (this is JKJavaPrimitiveType)
        PrimitiveType.valueOf(jvmPrimitiveType.name).arrayTypeFqName.asString()
    else KotlinBuiltIns.FQ_NAMES.array.asString()

fun JKClassSymbol.isArrayType(): Boolean =
    fqName in arrayFqNames

private val arrayFqNames = JKJavaPrimitiveType.KEYWORD_TO_INSTANCE.values
    .filterIsInstance<JKJavaPrimitiveType>()
    .map { PrimitiveType.valueOf(it.jvmPrimitiveType.name).arrayTypeFqName.asString() } +
        KotlinBuiltIns.FQ_NAMES.array.asString()

fun JKType.isArrayType() =
    when (this) {
        is JKClassType -> classReference.isArrayType()
        is JKJavaArrayType -> true
        else -> false
    }

val JKType.isCollectionType: Boolean
    get() = safeAs<JKClassType>()?.classReference?.fqName in collectionFqNames

private val collectionFqNames = setOf(
    KotlinBuiltIns.FQ_NAMES.mutableIterator.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableList.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableCollection.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableSet.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableMap.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableMapEntry.asString(),
    KotlinBuiltIns.FQ_NAMES.mutableListIterator.asString()
)

fun JKType.arrayInnerType(): JKType? =
    when (this) {
        is JKJavaArrayType -> type
        is JKClassType ->
            if (this.classReference.isArrayType()) this.parameters.singleOrNull()
            else null
        else -> null
    }

fun JKClassSymbol.isInterface(): Boolean {
    val target = target
    return when (target) {
        is PsiClass -> target.isInterface
        is KtClass -> target.isInterface()
        is JKClass -> target.classKind == JKClass.ClassKind.INTERFACE
        else -> false
    }
}

fun JKType.isInterface(): Boolean =
    (this as? JKClassType)?.classReference?.isInterface() ?: false


fun JKType.replaceJavaClassWithKotlinClassType(symbolProvider: JKSymbolProvider): JKType =
    applyRecursive { type ->
        if (type is JKClassType && type.classReference.fqName == "java.lang.Class") {
            JKClassType(
                symbolProvider.provideClassSymbol(KotlinBuiltIns.FQ_NAMES.kClass.toSafe()),
                type.parameters.map { it.replaceJavaClassWithKotlinClassType(symbolProvider) },
                Nullability.NotNull
            )
        } else null
    }
