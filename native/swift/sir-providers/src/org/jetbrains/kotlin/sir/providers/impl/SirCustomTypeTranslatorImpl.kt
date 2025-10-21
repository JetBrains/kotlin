/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isBooleanType
import org.jetbrains.kotlin.analysis.api.components.isByteType
import org.jetbrains.kotlin.analysis.api.components.isCharType
import org.jetbrains.kotlin.analysis.api.components.isClassType
import org.jetbrains.kotlin.analysis.api.components.isDoubleType
import org.jetbrains.kotlin.analysis.api.components.isFloatType
import org.jetbrains.kotlin.analysis.api.components.isIntType
import org.jetbrains.kotlin.analysis.api.components.isLongType
import org.jetbrains.kotlin.analysis.api.components.isPrimitive
import org.jetbrains.kotlin.analysis.api.components.isShortType
import org.jetbrains.kotlin.analysis.api.components.isStringType
import org.jetbrains.kotlin.analysis.api.components.isUByteType
import org.jetbrains.kotlin.analysis.api.components.isUIntType
import org.jetbrains.kotlin.analysis.api.components.isULongType
import org.jetbrains.kotlin.analysis.api.components.isUShortType
import org.jetbrains.kotlin.analysis.api.components.isUnitType
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
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSArray
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSDictionary
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSSet
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsObjCBridged
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.CType
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.KotlinType
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.bridgeAsNSCollectionElement
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
            FqNames._char.toSafe(),
            FqNames._byte.toSafe(),
            FqNames._short.toSafe(),
            FqNames._int.toSafe(),
            FqNames._long.toSafe(),
            FqNames.uByteFqName,
            FqNames.uShortFqName,
            FqNames.uIntFqName,
            FqNames.uLongFqName,
            FqNames._boolean.toSafe(),
            FqNames._double.toSafe(),
            FqNames._float.toSafe(),
            FqNames.unit.toSafe(),
        )

    public override fun isFqNameSupported(fqName: FqName): Boolean {
        return supportedFqNames.contains(fqName)
    }

    private val typeToWrapperMap = mapOf(
        SirSwiftModule.utf16CodeUnit to Pair(KotlinType.Char, CType.UInt16),

        SirSwiftModule.int8 to Pair(KotlinType.Byte, CType.Int8),
        SirSwiftModule.int16 to Pair(KotlinType.Short, CType.Int16),
        SirSwiftModule.int32 to Pair(KotlinType.Int, CType.Int32),
        SirSwiftModule.int64 to Pair(KotlinType.Long, CType.Int64),

        SirSwiftModule.uint8 to Pair(KotlinType.UByte, CType.UInt8),
        SirSwiftModule.uint16 to Pair(KotlinType.UShort, CType.UInt16),
        SirSwiftModule.uint32 to Pair(KotlinType.UInt, CType.UInt32),
        SirSwiftModule.uint64 to Pair(KotlinType.ULong, CType.UInt64),

        SirSwiftModule.bool to Pair(KotlinType.Boolean, CType.Bool),

        SirSwiftModule.double to Pair(KotlinType.Double, CType.Double),
        SirSwiftModule.float to Pair(KotlinType.Float, CType.Float),
    ).entries.associateTo(mutableMapOf()) { (declaration, kctype) ->
        SirNominalType(declaration) to Bridge.AsIs(
            declaration, kctype.first, kctype.second
        ).wrapper()
    }.also {
        it[SirNominalType(SirSwiftModule.void)] = Bridge.AsVoid.wrapper()
    }

    context(kaSession: KaSession)
    public override fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): SirCustomTypeTranslator.BridgeWrapper? {
        toPrimitiveTypeBridge()?.let { return it }
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

            isClassType(ClassId.topLevel(openEndRangeFqName)) -> {
                val typeArgument = typeArguments.single()
                if (typeArgument is KaTypeArgumentWithVariance && !typeArgument.type.isPrimitive) return null
                swiftType = SirNominalType(
                    SirSwiftModule.range,
                    listOf(typeArgument.sirType(ctx))
                )
                Bridge.AsObject(swiftType, KotlinType.KotlinObject, CType.Object).wrapper()
            }

            isClassType(ClassId.topLevel(closedRangeFqName)) -> {
                val typeArgument = typeArguments.single()
                if (typeArgument is KaTypeArgumentWithVariance && !typeArgument.type.isPrimitive) return null
                swiftType = SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(typeArgument.sirType(ctx))
                )
                Bridge.AsObject(swiftType, KotlinType.KotlinObject, CType.Object).wrapper()
            }

            isClassType(StandardClassIds.IntRange) -> {
                swiftType = SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(SirNominalType(SirSwiftModule.int32))
                )
                Bridge.AsObject(swiftType, KotlinType.KotlinObject, CType.Object).wrapper()
            }

            isClassType(StandardClassIds.LongRange) -> {
                swiftType = SirNominalType(
                    SirSwiftModule.closedRange,
                    listOf(SirNominalType(SirSwiftModule.int64))
                )
                Bridge.AsObject(swiftType, KotlinType.KotlinObject, CType.Object).wrapper()
            }
            else -> return null
        }.also {
            typeToWrapperMap[swiftType] = it
        }
    }

    private fun Bridge.wrapper(): SirCustomTypeTranslator.BridgeWrapper = SirCustomTypeTranslator.BridgeWrapper(this)

    context(kaSession: KaSession)
    private fun KaUsualClassType.toPrimitiveTypeBridge(): SirCustomTypeTranslator.BridgeWrapper? {
        val declaration = when {
            isCharType -> SirSwiftModule.utf16CodeUnit

            isByteType -> SirSwiftModule.int8
            isShortType -> SirSwiftModule.int16
            isIntType -> SirSwiftModule.int32
            isLongType -> SirSwiftModule.int64

            isUByteType -> SirSwiftModule.uint8
            isUShortType -> SirSwiftModule.uint16
            isUIntType -> SirSwiftModule.uint32
            isULongType -> SirSwiftModule.uint64

            isBooleanType -> SirSwiftModule.bool

            isDoubleType -> SirSwiftModule.double
            isFloatType -> SirSwiftModule.float

            isUnitType -> SirSwiftModule.void
            else -> return null
        }
        return SirNominalType(declaration).toBridge()
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

    override fun SirNominalType.toBridge(): SirCustomTypeTranslator.BridgeWrapper? {
        return typeToWrapperMap[this]
    }
}
