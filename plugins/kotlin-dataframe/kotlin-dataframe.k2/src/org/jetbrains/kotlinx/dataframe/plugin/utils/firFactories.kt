package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.PropertyName

internal fun FirDeclarationGenerationExtension.generateExtensionProperty(
    callableIdOrSymbol: CallableIdOrSymbol,
    receiverType: ConeClassLikeTypeImpl,
    propertyName: PropertyName,
    returnTypeRef: FirResolvedTypeRef,
    symbol: FirClassSymbol<*>? = null,
    effectiveVisibility: EffectiveVisibility = EffectiveVisibility.Public,
    source: KtSourceElement?,
    typeParameters: List<FirTypeParameter> = emptyList(),
): FirProperty {
    val firPropertySymbol = when (callableIdOrSymbol) {
        is CallableIdOrSymbol.Id -> FirRegularPropertySymbol(callableIdOrSymbol.callableId)
        is CallableIdOrSymbol.Symbol -> callableIdOrSymbol.symbol
    }

    return buildProperty {
        this.source = source
        propertyName.columnNameAnnotation?.let {
            annotations += it
        }
        moduleData = session.moduleData
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            effectiveVisibility
        )
        this.typeParameters += typeParameters
        this.returnTypeRef = returnTypeRef
        receiverParameter = buildReceiverParameter {
            this.symbol = FirReceiverParameterSymbol()
            containingDeclarationSymbol = firPropertySymbol
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            typeRef = receiverType.toFirResolvedTypeRef()
        }
        val classId = firPropertySymbol.callableId.classId
        if (classId != null) {
            dispatchReceiverType = if (symbol != null) {
                ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagWithFixedSymbol(classId, symbol),
                    emptyArray(),
                    false
                )
            } else {
                ConeClassLikeTypeImpl(
                    ConeClassLikeLookupTagImpl(classId),
                    emptyArray(),
                    false
                )
            }
        }
        val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
        getter = buildPropertyAccessor {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            this.returnTypeRef = returnTypeRef
            dispatchReceiverType = receiverType
            this.symbol = firPropertyAccessorSymbol
            this.propertySymbol = firPropertySymbol
            isGetter = true
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                effectiveVisibility
            )
        }
        name = propertyName.identifier
        this.symbol = firPropertySymbol
        isVar = false
    }
}

internal sealed interface CallableIdOrSymbol {
    class Id(val callableId: CallableId) : CallableIdOrSymbol
    class Symbol(val symbol: FirRegularPropertySymbol) : CallableIdOrSymbol
}