/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.types

import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

internal interface KtFirType : KtType, ValidityTokenOwner {
    val coneType: ConeKotlinType

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.render() }
}

internal class KtFirUsualClassType(
    coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtUsualClassType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        firBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag) ?: error("Class ${coneType.lookupTag} was not found")
    }
    override val typeArguments: List<KtTypeArgument> by cached {
        coneType.typeArguments.map { typeArgument ->
            firBuilder.buildTypeArgument(typeArgument)
        }
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

    override fun asString(): String = withValidityAssertion {
        coneType.render() //todo
    }
}

internal class KtFirFunctionalType(
    coneType: ConeClassLikeTypeImpl,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtFunctionalType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol by cached {
        firBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag) ?: error("Class ${coneType.lookupTag} was not found")
    }
    override val typeArguments: List<KtTypeArgument> by cached {
        coneType.typeArguments.map { typeArgument ->
            firBuilder.buildTypeArgument(typeArgument)
        }
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

    override val isSuspend: Boolean get() = withValidityAssertion { coneType.isSuspendFunctionType(firBuilder.rootSession) }
    override val arity: Int
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) coneType.typeArguments.size - 2
            else coneType.typeArguments.size - 1
        }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            if (coneType.isExtensionFunctionType) (typeArguments.first() as KtTypeArgumentWithVariance).type
            else null
        }

    override val parameterTypes: List<KtType> by cached {
        val parameterTypeArgs = if (coneType.isExtensionFunctionType) typeArguments.subList(1, typeArguments.lastIndex)
        else typeArguments.subList(0, typeArguments.lastIndex)
        parameterTypeArgs.map { (it as KtTypeArgumentWithVariance).type }
    }

    override val returnType: KtType
        get() = withValidityAssertion { (typeArguments.last() as KtTypeArgumentWithVariance).type }

    override fun asString(): String = withValidityAssertion {
        coneType.render() //todo
    }
}

internal class KtFirErrorType(
    coneType: ConeClassErrorType,
    override val token: ValidityToken,
) : KtErrorType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val error: String get() = withValidityAssertion { coneType.diagnostic.reason }
}

internal class KtFirTypeParameterType(
    coneType: ConeTypeParameterType,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtTypeParameterType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val name: Name get() = withValidityAssertion { coneType.lookupTag.name }
    override val symbol: KtTypeParameterSymbol by cached {
        firBuilder.buildTypeParameterSymbolByLookupTag(coneType.lookupTag)
            ?: error("Type parameter ${coneType.lookupTag} was not found")
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { KtTypeNullability.create(coneType.isNullable) }

    override fun asString(): String = withValidityAssertion {
        coneType.render() //todo
    }
}

internal class KtFirFlexibleType(
    coneType: ConeFlexibleType,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtFlexibleType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val lowerBound: KtType by cached { firBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KtType by cached { firBuilder.buildKtType(coneType.upperBound) }
}

internal class KtFirIntersectionType(
    coneType: ConeIntersectionType,
    override val token: ValidityToken,
    private val firBuilder: KtSymbolByFirBuilder,
) : KtIntersectionType(), KtFirType {
    override val coneType by weakRef(coneType)

    override val conjuncts: List<KtType> by cached {
        coneType.intersectedTypes.map { conjunct -> firBuilder.buildKtType(conjunct) }
    }
}

internal class KtFirTypeArgumentWithVariance(
    override val type: KtType,
    override val variance: Variance
) : KtTypeArgumentWithVariance()