package org.jetbrains.kotlinx.dataframe.plugin.utils

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlinx.dataframe.plugin.extensions.DataFramePlugin
import org.jetbrains.kotlinx.dataframe.plugin.extensions.impl.PropertyName

internal fun FirDeclarationGenerationExtension.generateExtensionProperty(
    callableId: CallableId,
    receiverType: ConeClassLikeTypeImpl,
    propertyName: PropertyName,
    returnTypeRef: FirResolvedTypeRef,
    symbol: FirClassSymbol<*>? = null,
    effectiveVisibility: EffectiveVisibility = EffectiveVisibility.Public,
    source: KtSourceElement?
): FirProperty {
    val firPropertySymbol = FirPropertySymbol(callableId)
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
        this.returnTypeRef = returnTypeRef
        receiverParameter = buildReceiverParameter {
            typeRef = receiverType.toFirResolvedTypeRef()
        }
        val classId = callableId.classId
        if (classId != null) {
            dispatchReceiverType = if (symbol != null) {
                ConeClassLikeTypeImpl(
                    ConeClassLookupTagWithFixedSymbol(classId, symbol),
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
            propertySymbol = firPropertySymbol
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
        isLocal = false
    }
}
