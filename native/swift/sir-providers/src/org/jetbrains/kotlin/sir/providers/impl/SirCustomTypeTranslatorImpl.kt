/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl.BridgeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaStandardTypeClassIds.BYTE
import org.jetbrains.kotlin.analysis.api.components.KaStandardTypeClassIds.INT
import org.jetbrains.kotlin.analysis.api.components.KaStandardTypeClassIds.LONG
import org.jetbrains.kotlin.analysis.api.components.KaStandardTypeClassIds.SHORT
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
import org.jetbrains.kotlin.sir.SirExistentialType
import org.jetbrains.kotlin.sir.SirFunctionBridge
import org.jetbrains.kotlin.sir.SirFunctionalType
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.SirOptionalType
import org.jetbrains.kotlin.sir.SirType
import org.jetbrains.kotlin.sir.providers.SirCustomTypeTranslator
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTypeNamer
import org.jetbrains.kotlin.sir.SirBridge
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSArray
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSDictionary
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsNSSet
import org.jetbrains.kotlin.sir.providers.impl.BridgeProvider.Bridge.AsObjCBridged
import org.jetbrains.kotlin.sir.providers.impl.SirTypeProviderImpl.TypeTranslationCtx
import org.jetbrains.kotlin.sir.providers.sirModule
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeSupportModule
import org.jetbrains.kotlin.sir.util.SirSwiftModule

public class SirCustomTypeTranslatorImpl(
    private val session: SirSession
) : SirCustomTypeTranslator {
    private val dynamicTypeToWrapperMap = mutableMapOf<SirNominalType, SirCustomTypeTranslator.BridgeWrapper>()

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
                    if (!swiftArgumentType.isBridgeableCollectionElement()) return null
                    swiftType = SirArrayType(
                        swiftArgumentType,
                    )
                    AsNSArray(swiftType, bridgeAsNSCollectionElement(swiftArgumentType)).wrapper()
                }

                isClassType(StandardClassIds.Set) -> {
                    val swiftArgumentType = typeArguments.single().sirType(ctx.copy(requiresHashableAsAny = true))
                    if (swiftArgumentType.containsExistential()) return null
                    if (!swiftArgumentType.isBridgeableCollectionElement()) return null
                    swiftType = SirNominalType(
                        SirSwiftModule.set,
                        listOf(swiftArgumentType)
                    )
                    AsNSSet(swiftType, bridgeAsNSCollectionElement(swiftArgumentType)).wrapper()
                }

                isClassType(StandardClassIds.Map) -> {
                    val swiftKeyType = typeArguments.first().sirType(ctx.copy(requiresHashableAsAny = true))
                    if (swiftKeyType.containsExistential()) return null
                    val swiftValueType = typeArguments.last().sirType(ctx)
                    if (!swiftKeyType.isBridgeableCollectionElement() || !swiftValueType.isBridgeableCollectionElement()) return null
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
                dynamicTypeToWrapperMap[swiftType] = it
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
        return primitiveTypeToWrapperMap[this] ?: dynamicTypeToWrapperMap[this]
    }

    internal class RangeBridge private constructor(
        override val swiftType: SirNominalType,
        val uniqueModuleName: String,
        val kotlinRangeClassId: ClassId,
        val kotlinRangeElementClassId: ClassId,
        val inclusive: Boolean,
    ) : BidirectionalBridge {
        override val kotlinType = KotlinType.KotlinObject
        override val cType = CType.Object

        companion object {
            context(session: SirSession)
            operator fun invoke(
                swiftType: SirNominalType,
                kotlinRangeClassId: ClassId,
                kotlinRangeElementClassId: ClassId,
                inclusive: Boolean,
            ): RangeBridge = RangeBridge(
                swiftType = swiftType,
                uniqueModuleName = session.moduleToTranslate.sirModule().name,
                kotlinRangeClassId = kotlinRangeClassId,
                kotlinRangeElementClassId = kotlinRangeElementClassId,
                inclusive = inclusive,
            )
        }

        private val elementTypes = classIdToWrapperMap[kotlinRangeElementClassId]!!

        val pairedParameterKotlinType: KotlinType
            get() = elementTypes.first

        val pairedParameterCType: CType
            get() = elementTypes.second

        private val kotlinRangeTypeName = kotlinRangeClassId.shortClassName.asString()
        private val kotlinRangeNameDecapitalized = kotlinRangeTypeName.replaceFirstChar(Char::lowercase)
        private val kotlinRangeElementNameDecapitalized = pairedParameterKotlinType.repr.replaceFirstChar(Char::lowercase)

        private val kotlinRangeTypeDescription = when (kotlinRangeClassId) {
            StandardClassIds.IntRange, StandardClassIds.LongRange -> kotlinRangeTypeName
            else -> "$kotlinRangeTypeName<${pairedParameterKotlinType.repr}>"
        }

        override val inKotlinSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "kotlin.native.internal.ref.dereferenceExternalRCRef($valueExpression) as ${
                    typeNamer.kotlinFqName(swiftType, SirTypeNamer.KotlinNameType.PARAMETRIZED)
                }"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String =
                "kotlin.native.internal.ref.createRetainedExternalRCRef($valueExpression)"
        }

        override val inSwiftSources = object : ValueConversion {
            context(session: SirSession)
            override fun swiftToKotlin(typeNamer: SirTypeNamer, valueExpression: String): String =
                "${constructorBridge().name}($valueExpression.lowerBound, $valueExpression.upperBound)"

            context(session: SirSession)
            override fun kotlinToSwift(typeNamer: SirTypeNamer, valueExpression: String): String {
                val startBridge = getterBridge(0)
                val endBridge = getterBridge(1)
                val operator = if (inclusive) "..." else "..<"
                return "{ let _ref = $valueExpression; return ${startBridge.name}(_ref) $operator ${endBridge.name}(_ref) }()"
            }
        }

        context(sir: SirSession)
        override fun helperBridges(typeNamer: SirTypeNamer): List<SirBridge> =
            listOf(constructorBridge(), getterBridge(0), getterBridge(1))

        private fun constructorBridge(): SirFunctionBridge {
            val operator = if (inclusive) ".." else "..<"
            val cRangeElementName = pairedParameterCType.render("")
            val name = "kotlin_ranges_${kotlinRangeNameDecapitalized}_create_${kotlinRangeElementNameDecapitalized}_$uniqueModuleName"
            return SirFunctionBridge(
                name,
                KotlinFunctionBridge(
                    lines = listOf(
                        "@${exportAnnotationFqName.substringAfterLast('.')}(\"$name\")",
                        "fun $name(start: ${pairedParameterKotlinType.repr}, end: ${pairedParameterKotlinType.repr}): kotlin.native.internal.NativePtr {",
                        "    return kotlin.native.internal.ref.createRetainedExternalRCRef(start $operator end)",
                        "}",
                    ),
                    packageDependencies = listOf()
                ),
                CFunctionBridge(
                    listOf("void * $name($cRangeElementName start, $cRangeElementName end);"),
                    listOf()
                )
            )
        }

        private fun getterBridge(index: Int): SirFunctionBridge {
            val propertyName = if (index == 0) "start" else "end${if (inclusive) "Inclusive" else "Exclusive"}"
            val propertyNameCapitalized = propertyName.replaceFirstChar(Char::uppercase)
            val cRangeElementName = pairedParameterCType.render("")
            val name = "kotlin_ranges_${kotlinRangeNameDecapitalized}_get${propertyNameCapitalized}_${kotlinRangeElementNameDecapitalized}_$uniqueModuleName"

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
    }

    private fun SirType.containsExistential(): Boolean = when (this) {
        is SirExistentialType -> protocols.isNotEmpty() && protocols.singleOrNull()?.first != KotlinRuntimeSupportModule.kotlinBridgeable
        is SirOptionalType -> wrappedType.containsExistential()
        else -> false
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

        private val primitiveTypeToWrapperMap: Map<SirNominalType, SirCustomTypeTranslator.BridgeWrapper> = buildMap {
            for ((declaration, kctype) in mapOf(
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
            )) {
                put(SirNominalType(declaration), Bridge.AsIs(declaration, kctype.first, kctype.second).wrapper())
            }
            put(SirNominalType(SirSwiftModule.void), Bridge.AsVoid.wrapper())
        }

        private val classIdToWrapperMap = hashMapOf(
            LONG to Pair(KotlinType.Long, CType.Int64),
            INT to Pair(KotlinType.Int, CType.Int32),
            SHORT to Pair(KotlinType.Short, CType.Int16),
            BYTE to Pair(KotlinType.Byte, CType.Int8),
            ClassId.fromString("kotlin/ULong") to Pair(KotlinType.ULong, CType.UInt64),
            ClassId.fromString("kotlin/UInt") to Pair(KotlinType.UInt, CType.UInt32),
            ClassId.fromString("kotlin/UShort") to Pair(KotlinType.UShort, CType.UInt16),
            ClassId.fromString("kotlin/UByte") to Pair(KotlinType.UByte, CType.UInt8),
        )

        private fun BidirectionalBridge.wrapper(): SirCustomTypeTranslator.BridgeWrapper = SirCustomTypeTranslator.BridgeWrapper(this)
    }
}

private fun SirType.isBridgeableCollectionElement(): Boolean = when (this) {
    is SirNominalType, is SirExistentialType, is SirFunctionalType -> true
    else -> false
}
