/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isClassType
import org.jetbrains.kotlin.analysis.api.components.isStringType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.SirArrayType
import org.jetbrains.kotlin.sir.SirDictionaryType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSArray
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSDictionary
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSSet
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsObjCBridged
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.CType
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.bridgeAsNSCollectionElement
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl.TypeTranslationCtx
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirCustomTypeTranslatorImpl(
    private val session: SirSession
) : SirCustomTypeTranslator {
    // These classes already have ObjC counterparts assigned statically in ObjC Export.
    private val supportedFqNames: List<FqName> =
        listOf(
            FqNames.set,
            FqNames.map,
            FqNames.list,
            FqNames.string.toSafe()
        )

    public override fun isFqNameSupported(fqName: FqName): Boolean {
        return supportedFqNames.contains(fqName)
    }

    private val typeToWrapperMap = mutableMapOf<SirNominalType, SirCustomTypeTranslator.BridgeWrapper>()

    context(kaSession: KaSession)
    public override fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): SirCustomTypeTranslator.BridgeWrapper? {
        var swiftType: SirNominalType
        return when {
            isStringType -> {
                swiftType = SirNominalType(SirSwiftModule.string)
                AsObjCBridged(swiftType, CType.NSString).wrapper()
            }
            isClassType(StandardClassIds.List) -> {
                val swiftArgumentType = typeArguments.single().sirType(ctx)
                swiftType = SirArrayType(
                    swiftArgumentType,
                )
                AsNSArray(swiftType, bridgeAsNSCollectionElement(swiftArgumentType, session)).wrapper()
            }

            isClassType(StandardClassIds.Set) -> {
                val swiftArgumentType = typeArguments.single().sirType(ctx.copy(requiresHashableAsAny = true))
                swiftType = SirNominalType(
                    SirSwiftModule.set,
                    listOf(swiftArgumentType)
                )
                AsNSSet(swiftType, bridgeAsNSCollectionElement(swiftArgumentType, session)).wrapper()
            }

            isClassType(StandardClassIds.Map) -> {
                val swiftKeyType = typeArguments.first().sirType(ctx.copy(requiresHashableAsAny = true))
                val swiftValueType = typeArguments.last().sirType(ctx)
                swiftType = SirDictionaryType(swiftKeyType, swiftValueType)
                AsNSDictionary(
                    swiftType,
                    bridgeAsNSCollectionElement(swiftKeyType, session),
                    bridgeAsNSCollectionElement(swiftValueType, session),
                ).wrapper()
            }

            else -> return null
        }.also {
            typeToWrapperMap[swiftType] = it
        }
    }

    private fun Bridge.wrapper(): SirCustomTypeTranslator.BridgeWrapper = SirCustomTypeTranslator.BridgeWrapper(this)

    context(kaSession: KaSession)
    private fun KaTypeProjection.sirType(ctx: TypeTranslationCtx): SirType = when (this) {
        is KaStarTypeProjection -> ctx.anyRepresentativeType()
        is KaTypeArgumentWithVariance -> with(session) {
            type.translateType(
                kaSession,
                ctx.currentPosition,
                ctx.reportErrorType,
                ctx.reportUnsupportedType,
                ctx.processTypeImports,
                ctx.requiresHashableAsAny,
            )
        }
    }

    override fun SirNominalType.toBridge(): SirCustomTypeTranslator.BridgeWrapper? {
        return typeToWrapperMap[this]
    }
}
