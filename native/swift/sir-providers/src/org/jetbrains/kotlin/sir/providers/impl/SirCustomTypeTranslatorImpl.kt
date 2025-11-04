/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds.BYTE
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds.INT
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds.LONG
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds.SHORT
import org.jetbrains.kotlin.analysis.api.components.isBooleanType
import org.jetbrains.kotlin.analysis.api.components.isByteType
import org.jetbrains.kotlin.analysis.api.components.isCharType
import org.jetbrains.kotlin.analysis.api.components.isClassType
import org.jetbrains.kotlin.analysis.api.components.isDoubleType
import org.jetbrains.kotlin.analysis.api.components.isFloatType
import org.jetbrains.kotlin.analysis.api.components.isIntType
import org.jetbrains.kotlin.analysis.api.components.isLongType
import org.jetbrains.kotlin.analysis.api.components.isShortType
import org.jetbrains.kotlin.analysis.api.components.isStringType
import org.jetbrains.kotlin.analysis.api.components.isUByteType
import org.jetbrains.kotlin.analysis.api.components.isUIntType
import org.jetbrains.kotlin.analysis.api.components.isULongType
import org.jetbrains.kotlin.analysis.api.components.isUShortType
import org.jetbrains.kotlin.analysis.api.components.isUnitType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.builtins.StandardNames.RANGES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.sir.CFunctionBridge
import org.jetbrains.kotlin.sir.KotlinFunctionBridge
import org.jetbrains.kotlin.sir.SirArrayType
import org.jetbrains.kotlin.sir.SirDictionaryType
import org.jetbrains.kotlin.sir.SirFunctionBridge
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSArray
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSDictionary
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSSet
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsObjCBridged
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl.TypeTranslationCtx
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirCustomTypeTranslatorImpl(
    private val session: SirSession
) : SirCustomTypeTranslator {
    public override fun isFqNameSupported(fqName: FqName): Boolean {
        return supportedFqNames.contains(fqName)
    }

    context(kaSession: KaSession)
    public override fun KaUsualClassType.toSirTypeBridge(ctx: TypeTranslationCtx): SirCustomTypeTranslator.BridgeWrapper? {
        toPrimitiveTypeBridge()?.let { return it }
        var swiftType: SirNominalType
        return context(session) {
            when {
                isStringType -> {
                    swiftType = SirNominalType(SirSwiftModule.string)
                    AsObjCBridged(swiftType, CType.NSString).wrapper()
                }
                isClassType(StandardClassIds.List) -> {
                    val swiftArgumentType = typeArguments.single().sirType(ctx)
                    swiftType = SirArrayType(
                        swiftArgumentType,
                    )
                    AsNSArray(swiftType, bridgeAsNSCollectionElement(swiftArgumentType)).wrapper()
                }

                isClassType(StandardClassIds.Set) -> {
                    val swiftArgumentType = typeArguments.single().sirType(ctx.copy(requiresHashableAsAny = true))
                    swiftType = SirNominalType(
                        SirSwiftModule.set,
                        listOf(swiftArgumentType)
                    )
                    AsNSSet(swiftType, bridgeAsNSCollectionElement(swiftArgumentType)).wrapper()
                }

                isClassType(StandardClassIds.Map) -> {
                    val swiftKeyType = typeArguments.first().sirType(ctx.copy(requiresHashableAsAny = true))
                    val swiftValueType = typeArguments.last().sirType(ctx)
                    swiftType = SirDictionaryType(swiftKeyType, swiftValueType)
                    AsNSDictionary(
                        swiftType,
                        bridgeAsNSCollectionElement(swiftKeyType),
                        bridgeAsNSCollectionElement(swiftValueType),
                    ).wrapper()
                }

                isClassType(ClassId.topLevel(openEndRangeFqName)) || isClassType(ClassId.topLevel(closedRangeFqName)) -> {
                    val argumentType = typeArguments.single()
                    if (argumentType is KaTypeArgumentWithVariance && !argumentType.type.isNumber) return null
                    val swiftArgumentType = argumentType.sirType(ctx)
                    val inclusive = !isClassType(ClassId.topLevel(openEndRangeFqName))
                    swiftType = SirNominalType(
                        typeDeclaration = if (inclusive) SirSwiftModule.closedRange else SirSwiftModule.range,
                        typeArguments = listOf(swiftArgumentType)
                    )
                    RangeBridge(
                        swiftType,
                        kotlinRangeClassId = classId,
                        kotlinRangeElementClassId = (argumentType.type as KaClassType).classId,
                        inclusive
                    ).wrapper()
                }

                isClassType(StandardClassIds.IntRange) -> {
                    val swiftArgumentType = SirNominalType(SirSwiftModule.int32)
                    swiftType = SirNominalType(
                        SirSwiftModule.closedRange,
                        listOf(swiftArgumentType)
                    )
                    RangeBridge(
                        swiftType,
                        kotlinRangeClassId = StandardClassIds.IntRange,
                        kotlinRangeElementClassId = StandardClassIds.Int,
                        inclusive = true
                    ).wrapper()
                }

                isClassType(StandardClassIds.LongRange) -> {
                    val swiftArgumentType = SirNominalType(SirSwiftModule.int64)
                    swiftType = SirNominalType(
                        SirSwiftModule.closedRange,
                        listOf(swiftArgumentType)
                    )
                    RangeBridge(
                        swiftType,
                        kotlinRangeClassId = StandardClassIds.LongRange,
                        kotlinRangeElementClassId = StandardClassIds.Long,
                        inclusive = true
                    ).wrapper()
                }
                else -> return null
            }.also {
                typeToWrapperMap[swiftType] = it
            }
        }
    }

    private val KaType.isNumber: Boolean
        get() = this is KaUsualClassType && this.classId in classIdToWrapperMap

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

    internal class RangeBridge(
        swiftType: SirNominalType,
        val kotlinRangeClassId: ClassId,
        val kotlinRangeElementClassId: ClassId,
        val inclusive: Boolean,
    ) : Bridge(
        swiftType,
        typeList = List(2) { classIdToWrapperMap[kotlinRangeElementClassId]!! },
    ) {
        val pairedParameterKotlinType: KotlinType
            get() = typeList.first().kotlinType

        val pairedParameterCType: CType
            get() = typeList.first().cType

        override val inKotlinSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String {
                val operator = if (inclusive) ".." else "..<"
                return "${valueExpression}_1 $operator ${valueExpression}_2"
            }

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "$valueExpression.lowerBound, $valueExpression.upperBound"

            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val startBridge = nativePointerToMultipleObjCBridge(0)
                val endBridge = nativePointerToMultipleObjCBridge(1)
                val operator = if (inclusive) "..." else "..<"
                return "${startBridge.name}($valueExpression) $operator ${endBridge.name}($valueExpression)"
            }
        }

        override fun nativePointerToMultipleObjCBridge(index: Int): SirFunctionBridge {
            when (index) {
                0, 1 -> {
                    val propertyName = if (index == 0) "start" else "end${if (inclusive) "Inclusive" else "Exclusive"}"
                    val propertyNameCapitalized = propertyName.replaceFirstChar(Char::uppercase)
                    val kotlinRangeTypeName = kotlinRangeClassId.shortClassName.asString()
                    val kotlinRangeNameDecapitalized = kotlinRangeTypeName.replaceFirstChar(Char::lowercase)
                    val kotlinRangeElementNameDecapitalized = pairedParameterKotlinType.repr.replaceFirstChar(Char::lowercase)
                    val cRangeElementName = pairedParameterCType.render("")
                    val name = "kotlin_ranges_${kotlinRangeNameDecapitalized}_get${propertyNameCapitalized}_$kotlinRangeElementNameDecapitalized"
                    val kotlinRangeTypeDescription = when (kotlinRangeClassId) {
                        StandardClassIds.IntRange, StandardClassIds.LongRange -> kotlinRangeTypeName
                        else -> "$kotlinRangeTypeName<${pairedParameterKotlinType.repr}>"
                    }

                    return SirFunctionBridge(
                        name,
                        KotlinFunctionBridge(
                            lines = listOf(
                                "@${exportAnnotationFqName.substringAfterLast('.')}(\"$name\")",
                                "fun $name(nativePtr: kotlin.native.internal.NativePtr): ${pairedParameterKotlinType.repr} {",
                                "    val $kotlinRangeNameDecapitalized = kotlin.native.internal.ref.dereferenceExternalRCRef(nativePtr) as $kotlinRangeTypeDescription",
                                "    return $kotlinRangeNameDecapitalized.$propertyName",
                                "}",
                            ),
                            packageDependencies = listOf()
                        ),
                        CFunctionBridge(
                            listOf("$cRangeElementName $name(void * nativePtr);"),
                            listOf()
                        )
                    )
                }

                else -> throw NoSuchElementException()
            }
        }
    }

    public companion object {
        private val openEndRangeFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier("OpenEndRange"))

        private val closedRangeFqName = RANGES_PACKAGE_FQ_NAME.child(Name.identifier("ClosedRange"))

        // These classes already have ObjC counterparts assigned statically in ObjC Export.
        private val supportedFqNames: List<FqName> =
            listOf(
                FqNames.set,
                FqNames.map,
                FqNames.list,
                FqNames.string.toSafe(),
                openEndRangeFqName,
                closedRangeFqName,
                FqNames.intRange.toSafe(),
                FqNames.longRange.toSafe(),
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

        private val typeToWrapperMap = mapOf(
            SirSwiftModule.utf16CodeUnit to AnyBridge.TypePair(KotlinType.Char, CType.UInt16),

            SirSwiftModule.int8 to AnyBridge.TypePair(KotlinType.Byte, CType.Int8),
            SirSwiftModule.int16 to AnyBridge.TypePair(KotlinType.Short, CType.Int16),
            SirSwiftModule.int32 to AnyBridge.TypePair(KotlinType.Int, CType.Int32),
            SirSwiftModule.int64 to AnyBridge.TypePair(KotlinType.Long, CType.Int64),

            SirSwiftModule.uint8 to AnyBridge.TypePair(KotlinType.UByte, CType.UInt8),
            SirSwiftModule.uint16 to AnyBridge.TypePair(KotlinType.UShort, CType.UInt16),
            SirSwiftModule.uint32 to AnyBridge.TypePair(KotlinType.UInt, CType.UInt32),
            SirSwiftModule.uint64 to AnyBridge.TypePair(KotlinType.ULong, CType.UInt64),

            SirSwiftModule.bool to AnyBridge.TypePair(KotlinType.Boolean, CType.Bool),

            SirSwiftModule.double to AnyBridge.TypePair(KotlinType.Double, CType.Double),
            SirSwiftModule.float to AnyBridge.TypePair(KotlinType.Float, CType.Float),
        ).entries.associateTo(mutableMapOf()) { (declaration, kctype) ->
            SirNominalType(declaration) to Bridge.AsIs(
                declaration, kctype.kotlinType, kctype.cType
            ).wrapper()
        }.also {
            it[SirNominalType(SirSwiftModule.void)] = Bridge.AsVoid.wrapper()
        }

        private val classIdToWrapperMap = hashMapOf(
            LONG to AnyBridge.TypePair(KotlinType.Long, CType.Int64),
            INT to AnyBridge.TypePair(KotlinType.Int, CType.Int32),
            SHORT to AnyBridge.TypePair(KotlinType.Short, CType.Int16),
            BYTE to AnyBridge.TypePair(KotlinType.Byte, CType.Int8),
            ClassId.fromString("kotlin/ULong") to AnyBridge.TypePair(KotlinType.ULong, CType.UInt64),
            ClassId.fromString("kotlin/UInt") to AnyBridge.TypePair(KotlinType.UInt, CType.UInt32),
            ClassId.fromString("kotlin/UShort") to AnyBridge.TypePair(KotlinType.UShort, CType.UInt16),
            ClassId.fromString("kotlin/UByte") to AnyBridge.TypePair(KotlinType.UByte, CType.UInt8),
        )

        private fun Bridge.wrapper(): SirCustomTypeTranslator.BridgeWrapper = SirCustomTypeTranslator.BridgeWrapper(this)
    }
}
