/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal fun ClassId.createDRI(): DRI = DRI(
    packageName = this.packageFqName.asString(), classNames = this.relativeClassName.asString()
)

private fun CallableId.createDRI(receiver: TypeReference?, params: List<TypeReference>): DRI = DRI(
    packageName = this.packageName.asString(),
    classNames = this.className?.asString(),
    callable = Callable(
        this.callableName.asString(),
        params = params,
        receiver = receiver
    )
)

internal fun getDRIFromNonErrorClassType(nonErrorClassType: KtNonErrorClassType): DRI =
    nonErrorClassType.classId.createDRI()

private val KtCallableSymbol.callableId
    get() = this.callableIdIfNonLocal ?: throw IllegalStateException("Can not get callable Id due to it is local")

// because of compatibility with Dokka K1, DRI of entry is kept as non-callable
internal fun getDRIFromEnumEntry(symbol: KtEnumEntrySymbol): DRI =
    symbol.callableId.let {
        DRI(
            packageName = it.packageName.asString(),
            classNames = it.className?.asString() + "." + it.callableName.asString(),
        )
    }.withEnumEntryExtra()


internal fun KtAnalysisSession.getDRIFromTypeParameter(symbol: KtTypeParameterSymbol): DRI {
    val containingSymbol =
        (symbol.getContainingSymbol() as? KtSymbolWithTypeParameters)
            ?: throw IllegalStateException("Containing symbol is null for type parameter")
    val typeParameters = containingSymbol.typeParameters
    val index = typeParameters.indexOfFirst { symbol.name == it.name }
    return getDRIFromSymbol(containingSymbol).copy(target = PointingToGenericParameters(index))
}

internal fun KtAnalysisSession.getDRIFromConstructor(symbol: KtConstructorSymbol): DRI =
    (symbol.containingClassIdIfNonLocal
        ?: throw IllegalStateException("Can not get class Id due to it is local")).createDRI().copy(
        callable = Callable(
            name = symbol.containingClassIdIfNonLocal?.shortClassName?.asString() ?: "",
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) })
    )

internal fun KtAnalysisSession.getDRIFromVariableLike(symbol: KtVariableLikeSymbol): DRI {
    val receiver = symbol.receiverType?.let {
        getTypeReferenceFrom(it)
    }
    return symbol.callableId.createDRI(receiver, emptyList())
}

internal fun KtAnalysisSession.getDRIFromFunctionLike(symbol: KtFunctionLikeSymbol): DRI {
    val params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) }
    val receiver = symbol.receiverType?.let {
        getTypeReferenceFrom(it)
    }
    return symbol.callableIdIfNonLocal?.createDRI(receiver, params)
        ?: getDRIFromLocalFunction(symbol)
}

internal fun getDRIFromClassLike(symbol: KtClassLikeSymbol): DRI =
    symbol.classIdIfNonLocal?.createDRI() ?: throw IllegalStateException("Can not get class Id due to it is local")

internal fun getDRIFromPackage(symbol: KtPackageSymbol): DRI =
    DRI(packageName = symbol.fqName.asString())

internal fun KtAnalysisSession.getDRIFromValueParameter(symbol: KtValueParameterSymbol): DRI {
    val function = (symbol.getContainingSymbol() as? KtFunctionLikeSymbol)
        ?: throw IllegalStateException("Containing symbol is null for type parameter")
    val index = function.valueParameters.indexOfFirst { it.name == symbol.name }
    val funDRI = getDRIFromFunctionLike(function)
    return funDRI.copy(target = PointingToCallableParameters(index))
}

/**
 * @return [DRI] to receiver type
 */
internal fun KtAnalysisSession.getDRIFromReceiverParameter(receiverParameterSymbol: KtReceiverParameterSymbol): DRI =
    getDRIFromReceiverType(receiverParameterSymbol.type)

private fun KtAnalysisSession.getDRIFromReceiverType(type: KtType): DRI {
    return when(type) {
        is KtNonErrorClassType -> getDRIFromNonErrorClassType(type)
        is KtTypeParameterType -> getDRIFromTypeParameter(type.symbol)
        is KtDefinitelyNotNullType -> getDRIFromReceiverType(type.original)
        is KtTypeErrorType -> DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type")
        is KtClassErrorType -> DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type")
        is KtDynamicType -> DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type") // prohibited by a compiler, but it's a possible input

        is KtCapturedType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        is KtFlexibleType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        is KtIntegerLiteralType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        is KtIntersectionType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
    }
}

internal fun KtAnalysisSession.getDRIFromSymbol(symbol: KtSymbol): DRI =
    when (symbol) {
        is KtEnumEntrySymbol -> getDRIFromEnumEntry(symbol)
        is KtTypeParameterSymbol -> getDRIFromTypeParameter(symbol)
        is KtConstructorSymbol -> getDRIFromConstructor(symbol)
        is KtValueParameterSymbol -> getDRIFromValueParameter(symbol)
        is KtVariableLikeSymbol -> getDRIFromVariableLike(symbol)
        is KtFunctionLikeSymbol -> getDRIFromFunctionLike(symbol)
        is KtClassLikeSymbol -> getDRIFromClassLike(symbol)
        is KtPackageSymbol -> getDRIFromPackage(symbol)
        is KtReceiverParameterSymbol -> getDRIFromReceiverParameter(symbol)
        else -> throw IllegalStateException("Unknown symbol while creating DRI $symbol")
    }

private fun KtAnalysisSession.getDRIFromNonCallablePossibleLocalSymbol(symbol: KtSymbol): DRI {
    if ((symbol as? KtSymbolWithKind)?.symbolKind == KtSymbolKind.LOCAL) {
        return symbol.getContainingSymbol()?.let { getDRIFromNonCallablePossibleLocalSymbol(it) }
            ?: throw IllegalStateException("Can't get containing symbol for local symbol")
    }
    return getDRIFromSymbol(symbol)
}

/**
 * Currently, it's used only for functions from enum entry,
 * For its members: `memberSymbol.callableIdIfNonLocal=null`
 */
private fun KtAnalysisSession.getDRIFromLocalFunction(symbol: KtFunctionLikeSymbol): DRI {
    /**
     * A function is inside local object
     */
    val containingSymbolDRI = symbol.getContainingSymbol()?.let { getDRIFromNonCallablePossibleLocalSymbol(it) }
        ?: throw IllegalStateException("Can't get containing symbol for local function")
    return containingSymbolDRI.copy(
        callable = Callable(
            (symbol as? KtNamedSymbol)?.name?.asString() ?: "",
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) },
            receiver = symbol.receiverType?.let {
                getTypeReferenceFrom(it)
            }
        )
    )
}

// ----------- DRI => compiler identifiers ----------------------------------------------------------------------------
internal fun getClassIdFromDRI(dri: DRI) = ClassId(
    FqName(dri.packageName ?: ""),
    FqName(dri.classNames ?: throw IllegalStateException("DRI must have `classNames` to get ClassID")),
    false
)

