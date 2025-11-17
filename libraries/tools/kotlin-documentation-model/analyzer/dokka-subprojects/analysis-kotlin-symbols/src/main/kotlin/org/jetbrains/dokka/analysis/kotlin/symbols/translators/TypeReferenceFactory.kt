/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.*

internal fun KaSession.getTypeReferenceFrom(type: KaType, isVararg: Boolean = false): TypeReference {
    val typeReference = getTypeReferenceFromPossiblyRecursive(type, emptyList())
    return when {
        isVararg -> Vararg(typeReference)
        else -> typeReference
    }
}


// see `deep recursive typebound #1342` test
private fun KaSession.getTypeReferenceFromPossiblyRecursive(
    type: KaType,
    paramTrace: List<KaType>
): TypeReference {
    if (type is KaTypeParameterType) {
        // compare by symbol since, e.g. T? and T have the different KaType, but the same type parameter
        paramTrace.indexOfFirst { it is KaTypeParameterType && type.symbol == it.symbol }
            .takeIf { it >= 0 }
            ?.let { return RecursiveType(it) }
    }

    return when (type) {
        is KaClassType -> TypeConstructor(
            type.classId.asFqNameString(),
            type.typeArguments.map {
                getTypeReferenceFromTypeProjection(
                    it,
                    paramTrace
                )
            }
        )

        is KaTypeParameterType -> {
            val upperBoundsOrNullableAny =
                type.symbol.upperBounds.takeIf { it.isNotEmpty() } ?: listOf(this.builtinTypes.nullableAny)

            TypeParam(
                name = type.name.asString(),
                bounds = upperBoundsOrNullableAny.map {
                    getTypeReferenceFromPossiblyRecursive(
                        it,
                        paramTrace + type
                    )
                }
            )
        }

        is KaClassErrorType -> TypeConstructor("$ERROR_CLASS_NAME $type", emptyList())

        is KaDefinitelyNotNullType -> getTypeReferenceFromPossiblyRecursive(
            type.original,
            paramTrace
        )

        is KaDynamicType -> TypeConstructor("[dynamic]", emptyList())
        is KaErrorType -> TypeConstructor("$ERROR_CLASS_NAME $type", emptyList())
        // Non-denotable types, see https://kotlinlang.org/spec/type-system.html#type-kinds

        // By the definition [flexible types](https://kotlinlang.org/spec/type-system.html#flexible-types),
        // we can take any type between lower and upper bounds
        // In Dokka K1, a lower bound is taken
        // For example, most Java types T in Kotlin are flexible (T..T?) or T!
        // see https://github.com/JetBrains/kotlin/blob/master/spec-docs/flexible-java-types.md (warn: it is not official spec)
        is KaFlexibleType -> getTypeReferenceFromPossiblyRecursive(
            type.lowerBound,
            paramTrace
        )

        is KaCapturedType -> throw NotImplementedError()
        is KaIntersectionType -> throw NotImplementedError()
        else -> throw NotImplementedError()
    }.let {
        if (type.isMarkedNullable) Nullable(it) else it
    }

}

private fun KaSession.getTypeReferenceFromTypeProjection(
    typeProjection: KaTypeProjection,
    paramTrace: List<KaType>
): TypeReference =
    when (typeProjection) {
        is KaStarTypeProjection -> StarProjection
        is KaTypeArgumentWithVariance -> getTypeReferenceFromPossiblyRecursive(typeProjection.type, paramTrace)
    }
