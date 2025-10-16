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
import org.jetbrains.kotlin.builtins.StandardNames.RANGES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.SirArrayType
import org.jetbrains.kotlin.sir.SirDictionaryType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl.TypeTranslationCtx
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirCustomTypeTranslatorImpl(
    private val session: SirSession
) : SirCustomTypeTranslator {
    private val openEndRangeFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier("OpenEndRange"))

    private val closedRangeFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier("ClosedRange"))

    // These classes already have ObjC counterparts assigned statically in ObjC Export.
    private val supportedFqNames: List<FqName> =
        listOf(
            FqNames.set,
//            FqNames.mutableSet,
            FqNames.map,
//            FqNames.mutableList,
            FqNames.list,
//            FqNames.mutableMap,
            FqNames.string.toSafe(),
            openEndRangeFqName,
            closedRangeFqName,
            FqNames.intRange.toSafe(),
            FqNames.longRange.toSafe(),
//            RANGES_PACKAGE_FQ_NAME.child(Name.identifier("UIntRange")),
//            RANGES_PACKAGE_FQ_NAME.child(Name.identifier("ULongRange")),
//            RANGES_PACKAGE_FQ_NAME.child(Name.identifier("CharRange")),
        )

    public override fun isFqNameSupported(fqName: FqName): Boolean {
        return supportedFqNames.contains(fqName)
    }

    context(kaSession: KaSession)
    public override fun KaUsualClassType.toSirType(ctx: TypeTranslationCtx): SirType? {
        return when {
            isStringType -> SirNominalType(SirSwiftModule.string)
            isClassType(StandardClassIds.List) -> {
                SirArrayType(
                    typeArguments.single().sirType(ctx),
                )
            }

            isClassType(StandardClassIds.Set) -> {
                SirNominalType(
                    SirSwiftModule.set,
                    listOf(typeArguments.single().sirType(ctx.copy(requiresHashableAsAny = true)))
                )
            }

            isClassType(StandardClassIds.Map) -> {
                SirDictionaryType(
                    typeArguments.first().sirType(ctx.copy(requiresHashableAsAny = true)),
                    typeArguments.last().sirType(ctx),
                )
            }

            isClassType(ClassId.topLevel(openEndRangeFqName)) -> {
                SirNominalType(
                    SirSwiftModule.range,
                    listOf(typeArguments.single().sirType(ctx))
                )
            }

            isClassType(ClassId.topLevel(closedRangeFqName)) -> {
                SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(typeArguments.single().sirType(ctx))
                )
            }

            isClassType(StandardClassIds.IntRange) -> {
                SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(SirNominalType(SirSwiftModule.int32))
                )
            }

            isClassType(StandardClassIds.LongRange) -> {
                SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(SirNominalType(SirSwiftModule.int64))
                )
            }
            else -> null
        }
    }

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
}
