/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.types

import com.intellij.psi.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.nj2k.JKSymbolProvider
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKTypeParameterSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUnresolvedClassSymbol
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable

class JKTypeFactory(val symbolProvider: JKSymbolProvider) {
    fun fromPsiType(type: PsiType): JKType = createPsiType(type)
    fun fromKotlinType(type: KotlinType): JKType = createKotlinType(type)

    inner class DefaultTypes {
        private fun typeByFqName(
            fqName: FqNameUnsafe,
            typeArguments: List<JKType> = emptyList(),
            nullability: Nullability = Nullability.NotNull
        ) = JKClassType(
            symbolProvider.provideClassSymbol(fqName),
            typeArguments,
            nullability
        )

        val boolean = typeByFqName(StandardNames.FqNames._boolean)
        val char = typeByFqName(StandardNames.FqNames._char)
        val byte = typeByFqName(StandardNames.FqNames._byte)
        val short = typeByFqName(StandardNames.FqNames._short)
        val int = typeByFqName(StandardNames.FqNames._int)
        val float = typeByFqName(StandardNames.FqNames._float)
        val long = typeByFqName(StandardNames.FqNames._long)
        val double = typeByFqName(StandardNames.FqNames._double)

        val string = typeByFqName(StandardNames.FqNames.string)
        val possiblyNullString = typeByFqName(StandardNames.FqNames.string, nullability = Nullability.Default)

        val unit = typeByFqName(StandardNames.FqNames.unit)
        val nothing = typeByFqName(StandardNames.FqNames.nothing)
        val nullableAny = typeByFqName(StandardNames.FqNames.any, nullability = Nullability.Nullable)

        val javaKlass = typeByFqName(FqNameUnsafe(CommonClassNames.JAVA_LANG_CLASS))
        val kotlinClass = typeByFqName(StandardNames.FqNames.kClass)
    }

    fun fromPrimitiveType(primitiveType: JKJavaPrimitiveType) = when (primitiveType.jvmPrimitiveType) {
        JvmPrimitiveType.BOOLEAN -> types.boolean
        JvmPrimitiveType.CHAR -> types.char
        JvmPrimitiveType.BYTE -> types.byte
        JvmPrimitiveType.SHORT -> types.short
        JvmPrimitiveType.INT -> types.int
        JvmPrimitiveType.FLOAT -> types.float
        JvmPrimitiveType.LONG -> types.long
        JvmPrimitiveType.DOUBLE -> types.double
    }

    val types by lazy(LazyThreadSafetyMode.NONE) { DefaultTypes() }

    private fun createPsiType(type: PsiType): JKType = when (type) {
        is PsiClassType -> {
            val target = type.resolve()
            val parameters = type.parameters.map { fromPsiType(it) }
            when (target) {
                null ->
                    JKClassType(JKUnresolvedClassSymbol(type.rawType().canonicalText, this), parameters)
                is PsiTypeParameter ->
                    JKTypeParameterType(symbolProvider.provideDirectSymbol(target) as JKTypeParameterSymbol)
                is PsiAnonymousClass -> {
                    /*
                     If anonymous class is declared inside the converting code, we will not be able to access JKUniverseClassSymbol's target
                     And get UninitializedPropertyAccessException exception, so it is ok to use base class for now
                    */
                    createPsiType(target.baseClassType)
                }
                else -> {
                    JKClassType(
                        target.let { symbolProvider.provideDirectSymbol(it) as JKClassSymbol },
                        parameters
                    )
                }
            }
        }
        is PsiArrayType -> JKJavaArrayType(fromPsiType(type.componentType))
        is PsiPrimitiveType -> JKJavaPrimitiveType.fromPsi(type)
        is PsiDisjunctionType ->
            JKJavaDisjunctionType(type.disjunctions.map { fromPsiType(it) })
        is PsiWildcardType ->
            when {
                type.isExtends ->
                    JKVarianceTypeParameterType(
                        JKVarianceTypeParameterType.Variance.OUT,
                        fromPsiType(type.extendsBound)
                    )
                type.isSuper ->
                    JKVarianceTypeParameterType(
                        JKVarianceTypeParameterType.Variance.IN,
                        fromPsiType(type.superBound)
                    )
                else -> JKStarProjectionTypeImpl
            }
        is PsiCapturedWildcardType ->
            JKCapturedType(fromPsiType(type.wildcard) as JKWildCardType)
        is PsiIntersectionType -> // TODO what to do with intersection types? old j2k just took the first conjunct
            fromPsiType(type.representative)
        is PsiLambdaParameterType -> // Probably, means that we have erroneous Java code
            JKNoType
        else -> throw Exception("Invalid PSI ${type::class.java}")
    }

    private fun createKotlinType(type: KotlinType): JKType {
        return when (val descriptor = type.constructor.declarationDescriptor) {
            is TypeParameterDescriptor ->
                JKTypeParameterType(
                    symbolProvider.provideDirectSymbol(
                        descriptor.findPsi() as? KtTypeParameter ?: return JKNoType
                    ) as JKTypeParameterSymbol
                )

            else -> JKClassType(
                symbolProvider.provideClassSymbol(type.getJetTypeFqName(false)),//TODO constructor fqName
                type.arguments.map { typeArgument ->
                    if (typeArgument.isStarProjection) JKStarProjectionTypeImpl
                    else fromKotlinType(typeArgument.type)
                },
                if (type.isNullable()) Nullability.Nullable else Nullability.NotNull
            )
        }
    }
}
