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
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingFieldAttr
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlinx.dataframe.plugin.DataFramePlugin

/**
 * @param generateJvmName - JvmName is needed to avoid clashes only between top level properties.
 * This problem is not relevant to extension properties in local classes
 */
internal fun FirDeclarationGenerationExtension.generateExtensionProperty(
    callableIdOrSymbol: CallableIdOrSymbol,
    receiverType: ConeClassLikeType,
    marker: ConeClassLikeType,
    propertyName: Name,
    returnType: ConeKotlinType,
    symbol: FirClassSymbol<*>? = null,
    effectiveVisibility: EffectiveVisibility = EffectiveVisibility.Public,
    source: KtSourceElement?,
    typeParameters: List<FirTypeParameter> = emptyList(),
    generateJvmName: Boolean = false
): FirProperty {
    val firPropertySymbol = when (callableIdOrSymbol) {
        is CallableIdOrSymbol.Id -> FirRegularPropertySymbol(callableIdOrSymbol.callableId)
        is CallableIdOrSymbol.Symbol -> callableIdOrSymbol.symbol
    }

    return buildProperty {
        this.source = source
        moduleData = session.moduleData
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            effectiveVisibility
        )
        isLocal = symbol?.isLocal == true
        this.typeParameters += typeParameters
        this.returnTypeRef = returnType.toFirResolvedTypeRef()
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
                ConeClassLikeLookupTagWithFixedSymbol(classId, symbol).constructClassType()
            } else {
                classId.constructClassLikeType()
            }
        }
        val firPropertyAccessorSymbol = FirPropertyAccessorSymbol()
        getter = buildPropertyAccessor {
            moduleData = session.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Plugin(DataFramePlugin)
            this.returnTypeRef = returnType.toFirResolvedTypeRef()
            dispatchReceiverType = receiverType
            this.symbol = firPropertyAccessorSymbol
            this.propertySymbol = firPropertySymbol
            isGetter = true
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                effectiveVisibility
            )
            runIf(generateJvmName) {
                annotations += buildPropertyJvmName(marker, propertyName)
            }
        }
        name = propertyName
        this.symbol = firPropertySymbol
        isVar = false
    }.also {
        @OptIn(FirImplementationDetail::class)
        it.hasBackingFieldAttr = false
    }
}

private fun buildPropertyJvmName(
    marker: ConeClassLikeType,
    propertyName: Name,
): FirAnnotation {
    val markerClassId = marker.lookupTag.classId
    val nestedName = markerClassId.relativeClassName.pathSegments().joinToString(separator = "_") {
        it.identifier
    }
    // class A { class B { val prop: Int; val B_prop: Int } }
    val jvmNameArg = "${nestedName}_${propertyName.identifier.replace("_", "_u")}"

    return buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef {
            coneType = StandardClassIds.Annotations.jvmName.constructClassLikeType()
        }
        argumentMapping = buildAnnotationArgumentMapping {
            mapping[StandardClassIds.Annotations.ParameterNames.parameterNameName] = buildLiteralExpression(
                source = null,
                kind = ConstantValueKind.String,
                value = jvmNameArg,
                setType = true,
            )
        }
    }
}

internal sealed interface CallableIdOrSymbol {
    class Id(val callableId: CallableId) : CallableIdOrSymbol
    class Symbol(val symbol: FirRegularPropertySymbol) : CallableIdOrSymbol
}
