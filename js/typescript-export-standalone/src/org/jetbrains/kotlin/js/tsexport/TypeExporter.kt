/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class, KaExperimentalApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedParameter
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType.*
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType.Array
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType.Function
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.memoryOptimizedMap

internal class TypeExporter(private val config: TypeScriptExportConfig, private val scope: TypeParameterScope) {
    /**
     * Memoize already processed types during recursive traversal of a type to avoid stack overflow on self-referential types,
     * like type parameters whose upper bound references the type parameter itself.
     * Note that despite what is said in [KaType]'s KDoc, it's fine to use a hash map here.
     * We don't need semantic equality, structural equality is enough, because types cannot be infinite structurally while being
     * finite semantically.
     */
    private val currentlyProcessedTypes = hashSetOf<KaType>()

    context(_: KaSession)
    internal fun exportType(type: KaType): ExportedType {
        if (type is KaDynamicType || type in currentlyProcessedTypes)
            return Primitive.Any

        if (type !is KaClassType && type !is KaTypeParameterType)
            @OptIn(KaExperimentalApi::class)
            return ErrorType("Unsupported type ${type.render(position = Variance.INVARIANT)}")

        currentlyProcessedTypes.add(type)

        val isMarkedNullable = type.isMarkedNullable
        val nonNullType = type.withNullability(isMarkedNullable = false)

        val exportedType = exportSimpleNonNullableType(nonNullType)

        return exportedType.withNullability(isMarkedNullable)
            .also { currentlyProcessedTypes.remove(type) }
    }

    context(_: KaSession)
    internal fun exportSpecializedArrayWithElementType(type: KaType): ExportedType = with(type) {
        when {
            isMarkedNullable -> Array(exportType(type))
            isByteType -> Primitive.ByteArray
            isShortType -> Primitive.ShortArray
            isIntType -> Primitive.IntArray
            isFloatType -> Primitive.FloatArray
            isDoubleType -> Primitive.DoubleArray
            isLongType -> if (config.compileLongAsBigInt) Primitive.LongArray else ErrorType("LongArray")
            isBooleanType -> ErrorType("BooleanArray")
            isCharType -> ErrorType("CharArray")
            else -> Array(exportType(type))
        }
    }

    context(_: KaSession)
    private fun exportSimpleNonNullableType(type: KaType): ExportedType {
        if (type.isBooleanType)
            return Primitive.Boolean
        if (type.isLongType || type.isULongType)
            return if (config.compileLongAsBigInt) Primitive.BigInt else ErrorType("Long")
        if (type.isPrimitive && !type.isCharType)
            return Primitive.Number
        if (type.isStringType)
            return Primitive.String
        if (type.isAnyType)
            return Primitive.Any
        if (type.isUnitType)
            return Primitive.Unit
        if (type.isNothingType)
            return Primitive.Nothing
        type.arrayElementType?.let {
            return if (type.isClassType(StandardClassIds.Array)) {
                Array(exportType(it))
            } else {
                exportSpecializedArrayWithElementType(it)
            }
        }
        if (type.isClassType(StandardClassIds.Throwable))
            return Primitive.Throwable
        if (type is KaFunctionType && !type.isKFunctionType && !type.isKSuspendFunctionType) {
            return if (type.isSuspend) {
                ErrorType("Suspend functions are not supported")
            } else {
                Function(
                    parameters = buildList {
                        type.contextReceivers.mapTo(this) {
                            ExportedParameter(
                                name = it.label?.asString(),
                                type = exportType(it.type),
                            )
                        }
                        type.receiverType?.let {
                            add(
                                ExportedParameter(
                                    name = SpecialNames.THIS.asString(),
                                    type = exportType(it),
                                )
                            )
                        }
                        type.parameters.mapTo(this) {
                            ExportedParameter(
                                name = it.name?.asString(),
                                type = exportType(it.type),
                            )
                        }
                    },
                    returnType = exportType(type.returnType),
                )
            }
        }
        if (type is KaTypeParameterType) {
            return scope[type.symbol]?.let(ExportedType::TypeParameterRef)
                ?: error("Type parameter ${type.symbol.render()} is not in scope")
        }
        if (type is KaClassType) {
            val symbol = type.symbol
            val isExported = shouldDeclarationBeExportedImplicitlyOrExplicitly(symbol)
            return when (symbol) {
                is KaNamedClassSymbol -> {
                    val isImplicitlyExported = !isExported && !symbol.isExternal
                    val isNonExportedExternal = symbol.isExternal && !isExported
                    val name = symbol
                        .getExportedFqName(shouldIncludePackage = !isNonExportedExternal && config.generateNamespacesForPackages, config)
                        .asString()

                    // TODO(KT-82340): Approximate to actual supertype
                    val exportedSupertype = Primitive.Any

                    val classType = ClassType(
                        name = name,
                        arguments = type.typeArguments.memoryOptimizedMap { exportTypeArgument(it) },
                        classId = symbol.classId,
                    )

                    when (symbol.classKind) {
                        KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> TypeOf(classType)
                        KaClassKind.CLASS, KaClassKind.ENUM_CLASS, KaClassKind.INTERFACE -> classType
                        KaClassKind.ANNOTATION_CLASS -> ErrorType("Annotation classes are not supported")
                        KaClassKind.ANONYMOUS_OBJECT -> ErrorType("Anonymous objects are not supported")
                    }.withImplicitlyExported(isImplicitlyExported, exportedSupertype)
                }
                is KaTypeAliasSymbol -> {
                    // TODO(KT-49795): Don't expand, export as a type alias reference instead.
                    exportType(type.fullyExpandedType)
                }
                is KaAnonymousObjectSymbol -> ErrorType("Anonymous objects are not supported")
            }
        }
        error("type must be either KaClassType or KaTypeParameterType. Actual type: ${type.javaClass.name}")
    }

    @OptIn(KaExperimentalApi::class)
    context(_: KaSession)
    fun exportTypeArgument(typeArgument: KaTypeProjection): ExportedType = when (typeArgument) {
        is KaTypeArgumentWithVariance -> exportType(typeArgument.type)
        is KaStarTypeProjection -> ErrorType("UnknownType *")
    }
}
