/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

internal fun ClassId.createDRI(): DRI = DRI(
    packageName = this.packageFqName.asString(), classNames = this.relativeClassName.asString()
)

private fun CallableId.createDRI(receiver: TypeReference?, params: List<TypeReference>, contextParams: List<TypeReference>): DRI = DRI(
    packageName = this.packageName.asString(),
    classNames = this.className?.asString(),
    callable = Callable(
        this.callableName.asString(),
        params = params,
        receiver = receiver,
        contextParameters = contextParams
    )
)

internal fun getDRIFromClassType(classType: KaClassType): DRI =
    classType.classId.createDRI()

// because of compatibility with Dokka K1, DRI of entry is kept as non-callable
internal fun getDRIFromEnumEntry(symbol: KaEnumEntrySymbol): DRI {
    val callableId = symbol.callableId ?: throw IllegalStateException("Can not get callable Id due to it is local")
    return DRI(
        packageName = callableId.packageName.asString(),
        classNames = callableId.className?.asString() + "." + callableId.callableName.asString(),
    ).withEnumEntryExtra()
}


@OptIn(KaExperimentalApi::class) // due to `typeParameters`
internal fun KaSession.getDRIFromTypeParameter(symbol: KaTypeParameterSymbol): DRI {
    val containingSymbol = symbol.containingSymbol
            ?: throw IllegalStateException("Containing symbol is null for type parameter")
    val typeParameters = (containingSymbol as KaDeclarationSymbol).typeParameters
    val index = typeParameters.indexOfFirst { symbol.name == it.name }
    return getDRIFromSymbol(containingSymbol).copy(target = PointingToGenericParameters(index))
}

internal fun KaSession.getDRIFromConstructor(symbol: KaConstructorSymbol): DRI {
    val containingClassId =
        symbol.containingClassId ?: throw IllegalStateException("Can not get class Id due to it is local")
    return containingClassId.createDRI().copy(
        callable = Callable(
            name = containingClassId.shortClassName.asString(),
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType, isVararg = it.isVararg) }
        )
    )
}

internal fun KaSession.getDRIFromVariable(symbol: KaVariableSymbol): DRI {
    val callableId = symbol.callableId ?: throw IllegalStateException("Can not get callable Id due to it is local")
    val receiver = symbol.receiverType?.let(::getTypeReferenceFrom)
    val contextParams = @OptIn(KaExperimentalApi::class) symbol.contextParameters.map { getTypeReferenceFrom(it.returnType) }
    return callableId.createDRI(receiver, emptyList(), contextParams)
}


internal fun KaSession.getDRIFromFunction(symbol: KaFunctionSymbol): DRI {
    val params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType, isVararg = it.isVararg) }
    val contextParams = @OptIn(KaExperimentalApi::class) symbol.contextParameters.map { getTypeReferenceFrom(it.returnType) }
    val receiver = symbol.receiverType?.let {
        getTypeReferenceFrom(it)
    }
    return symbol.callableId?.createDRI(receiver, params, contextParams) ?: getDRIFromLocalFunction(symbol)
}

internal fun getDRIFromClassLike(symbol: KaClassLikeSymbol): DRI =
    symbol.classId?.createDRI() ?: throw IllegalStateException("Can not get class Id due to it is local")

internal fun getDRIFromPackage(symbol: KaPackageSymbol): DRI =
    DRI(packageName = symbol.fqName.asString())

internal fun KaSession.getDRIFromValueParameter(symbol: KaValueParameterSymbol): DRI {
    val function = (symbol.containingSymbol as? KaFunctionSymbol)
        ?: throw IllegalStateException("Containing symbol is null for type parameter")
    val index = function.valueParameters.indexOfFirst { it.name == symbol.name }
    val funDRI = getDRIFromFunction(function)
    return funDRI.copy(target = PointingToCallableParameters(index))
}

/**
 * @return [DRI] to receiver type
 */
internal fun KaSession.getDRIFromReceiverParameter(receiverParameterSymbol: KaReceiverParameterSymbol): DRI =
    getDRIFromReceiverType(receiverParameterSymbol.returnType)

private fun KaSession.getDRIFromReceiverType(type: KaType): DRI {
    return when (type) {
        is KaClassType -> getDRIFromClassType(type)
        is KaTypeParameterType -> getDRIFromTypeParameter(type.symbol)
        is KaDefinitelyNotNullType -> getDRIFromReceiverType(type.original)
        is KaErrorType -> DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type")
        is KaDynamicType -> DRI(packageName = "", classNames = "$ERROR_CLASS_NAME $type") // prohibited by a compiler, but it's a possible input

        is KaCapturedType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        is KaFlexibleType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        is KaIntersectionType -> throw IllegalStateException("Unexpected non-denotable type while creating DRI $type")
        else -> throw IllegalStateException("Unexpected type while creating DRI $type")
    }
}

internal fun KaSession.getDRIFromSymbol(symbol: KaSymbol): DRI =
    when (symbol) {
        is KaEnumEntrySymbol -> getDRIFromEnumEntry(symbol)
        is KaReceiverParameterSymbol -> getDRIFromReceiverParameter(symbol)
        is KaTypeParameterSymbol -> getDRIFromTypeParameter(symbol)
        is KaConstructorSymbol -> getDRIFromConstructor(symbol)
        is KaValueParameterSymbol -> getDRIFromValueParameter(symbol)
        is KaVariableSymbol -> getDRIFromVariable(symbol)
        is KaFunctionSymbol -> getDRIFromFunction(symbol)
        is KaClassLikeSymbol -> getDRIFromClassLike(symbol)
        is KaPackageSymbol -> getDRIFromPackage(symbol)
        else -> throw IllegalStateException("Unknown symbol while creating DRI $symbol")
    }

private fun KaSession.getDRIFromNonCallablePossibleLocalSymbol(symbol: KaSymbol): DRI {
    if (symbol.location == KaSymbolLocation.LOCAL) {
        return symbol.containingSymbol?.let { getDRIFromNonCallablePossibleLocalSymbol(it) }
            ?: throw IllegalStateException("Can't get containing symbol for local symbol")
    }
    return getDRIFromSymbol(symbol)
}

/**
 * Currently, it's used only for functions from enum entry,
 * For its members: `memberSymbol.callableIdIfNonLocal=null`
 */
private fun KaSession.getDRIFromLocalFunction(symbol: KaFunctionSymbol): DRI {
    /**
     * A function is inside local object
     */
    val containingSymbolDRI = symbol.containingSymbol?.let { getDRIFromNonCallablePossibleLocalSymbol(it) }
        ?: throw IllegalStateException("Can't get containing symbol for local function")
    return containingSymbolDRI.copy(
        callable = Callable(
            (symbol as? KaNamedSymbol)?.name?.asString() ?: "",
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType, isVararg = it.isVararg) },
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

