/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.buildFunctionTypeParameter
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.*
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeArgumentListImpl
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance

/** Makes the implicit vararg array explicit: the resolved type includes it, but the written reference describes its element. */
internal fun FirTypeRef.asVarargArrayTypeRef(): FirResolvedTypeRef {
    val arrayType = coneType
    val elementSource = source ?: return arrayType.toFirResolvedTypeRef()
    val elementRef = asVarargElementTypeRef()
    return buildResolvedTypeRef {
        source = elementSource
        coneType = arrayType
        delegatedTypeRef = buildUserTypeRef {
            source = elementSource
            isMarkedNullable = false
            qualifier += FirQualifierPartImpl(source, StandardClassIds.Array.shortClassName, FirTypeArgumentListImpl(source).apply {
                typeArguments += buildTypeProjectionWithVariance {
                    typeRef = elementRef
                    variance = Variance.OUT_VARIANCE
                }
            })
        }
    }
}

/** Returns the written vararg element with its resolved type, excluding the implicit outer array. */
internal fun FirTypeRef.asVarargElementTypeRef(): FirResolvedTypeRef {
    val elementType = requireNotNull(coneType.arrayElementType(checkUnsignedArrays = false))
    val elementSource = source ?: return elementType.toFirResolvedTypeRef()
    return buildResolvedTypeRef {
        source = elementSource
        coneType = elementType
        delegatedTypeRef = unwrapResolvedTypeRef()
    }
}

/**
 * Builds a temporary FIR type reference whose syntax follows the already substituted expansion, not alias arguments.
 * The ordinary error-type corrector can therefore map arrays, function types, and wildcards exactly as for a non-aliased reference.
 *
 * For example, given `typealias Reversed<A, B> = Map<B, A>`, the written reference `Reversed<String, Int>`
 * has a resolved Cone type `Map<Int, String>` with `Reversed<String, Int>` stored in its abbreviation attribute.
 * This function builds a matching `Map<Int, String>` reference and removes abbreviation attributes from the
 * reconstructed class-like types. Thus, annotation paths address the expanded arguments (`Int` at index 0,
 * `String` at index 1), not the written alias arguments. FIR has already substituted the arguments and their
 * projection kinds; this function preserves those kinds while recursively rebuilding the argument types.
 *
 * Note that non-null `source` is required for constructing a FIR type reference but does not affect the reference structure,
 * so it can be any placeholder if the real source is not available, e.g. for inferred types.
 */
internal fun ConeKotlinType.toExpandedTypeRef(session: FirSession, source: KtSourceElement): FirResolvedTypeRef {
    if (this !is ConeClassLikeType) return toFirResolvedTypeRef()

    val argumentRefs = typeArguments.map { it.type?.toExpandedTypeRef(session, source) }
    val expandedType = withArguments(typeArguments.mapIndexed { index, projection ->
        projection.replaceType(argumentRefs[index]?.coneType)
    }.toTypedArray()).withAttributes(attributes.remove(AbbreviatedTypeAttribute::class))

    // Retain source qualifiers for unresolved imports/nested names. This does not establish an inner-class relationship.
    val errorQualifiers = if (this is ConeErrorType) {
        (diagnostic as? ConeUnresolvedTypeQualifierError)?.qualifiers?.takeIf { it.isNotEmpty() }
            ?: return expandedType.toFirResolvedTypeRef()
    } else null

    val kind = expandedType.functionTypeKind(session)
    val delegated = if (argumentRefs.isNotEmpty() && argumentRefs.all { it != null } &&
        (kind == FunctionTypeKind.Function || kind == FunctionTypeKind.SuspendFunction)
    ) {
        buildFunctionTypeRef {
            this.source = source
            isMarkedNullable = expandedType.isMarkedNullable
            isSuspend = kind == FunctionTypeKind.SuspendFunction
            // Context parameters and receivers already occupy their expanded JVM argument positions.
            parameters += argumentRefs.dropLast(1).map { argument ->
                buildFunctionTypeParameter {
                    this.source = source
                    returnTypeRef = requireNotNull(argument)
                }
            }
            returnTypeRef = requireNotNull(argumentRefs.last())
        }
    } else {
        buildUserTypeRef {
            this.source = source
            isMarkedNullable = expandedType.isMarkedNullable
            val name = errorQualifiers?.last()?.name ?: expandedType.lookupTag.classId.shortClassName
            // Preserve qualified missing types as written; there is no reliable enclosing-argument mapping.
            if (errorQualifiers != null && errorQualifiers.size > 1) {
                qualifier += errorQualifiers
                return@buildUserTypeRef
            }
            qualifier += FirQualifierPartImpl(source, name, FirTypeArgumentListImpl(source).apply {
                typeArguments += expandedType.typeArguments.mapIndexed { index, projection ->
                    val argument = argumentRefs[index]
                    if (argument == null) buildStarProjection()
                    else buildTypeProjectionWithVariance {
                        typeRef = argument
                        variance = projection.variance
                    }
                }
            })
        }
    }
    return buildResolvedTypeRef {
        this.source = source
        coneType = expandedType
        delegatedTypeRef = delegated
    }
}

internal fun FirTypeRef.unwrapResolvedTypeRef(): FirTypeRef? {
    var current: FirTypeRef? = this
    while (current is FirResolvedTypeRef) current = current.delegatedTypeRef
    return current
}
