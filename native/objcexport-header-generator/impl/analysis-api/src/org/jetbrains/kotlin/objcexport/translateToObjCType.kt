package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtErrorType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isError
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCObjectType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType


/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapType]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCType(typeBridge: TypeBridge): ObjCType {
    return when (typeBridge) {
        is ReferenceBridge -> this.translateToObjCReferenceType()
        is BlockPointerBridge -> this.translateToObjCFunctionType(typeBridge)
        is ValueTypeBridge -> when (typeBridge.objCValueType) {
            ObjCValueType.BOOL -> ObjCPrimitiveType.BOOL
            ObjCValueType.UNICHAR -> ObjCPrimitiveType.unichar
            ObjCValueType.CHAR -> ObjCPrimitiveType.int8_t
            ObjCValueType.SHORT -> ObjCPrimitiveType.int16_t
            ObjCValueType.INT -> ObjCPrimitiveType.int32_t
            ObjCValueType.LONG_LONG -> ObjCPrimitiveType.int64_t
            ObjCValueType.UNSIGNED_CHAR -> ObjCPrimitiveType.uint8_t
            ObjCValueType.UNSIGNED_SHORT -> ObjCPrimitiveType.uint16_t
            ObjCValueType.UNSIGNED_INT -> ObjCPrimitiveType.uint32_t
            ObjCValueType.UNSIGNED_LONG_LONG -> ObjCPrimitiveType.uint64_t
            ObjCValueType.FLOAT -> ObjCPrimitiveType.float
            ObjCValueType.DOUBLE -> ObjCPrimitiveType.double
            ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, isBinaryRepresentationNullable())
        }
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceType]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCReferenceType(): ObjCReferenceType {
    return mapToReferenceTypeIgnoringNullability().withNullabilityOf(this)
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.mapReferenceTypeIgnoringNullability]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtType.mapToReferenceTypeIgnoringNullability(): ObjCNonNullReferenceType {
    val fullyExpandedType = fullyExpandedType
    val classId = (fullyExpandedType as? KtNonErrorClassType)?.classId

    if (isError) {
        return objCErrorType
    }

    if (isAny) {
        return ObjCIdType
    }

    if (classId in hiddenClassIds) {
        return ObjCIdType
    }

    if (isObjCObjectType()) {
        return ObjCIdType
    }

    /* Check if inline type represents 'regular' inline class */
    run check@{
        if (classId == null) return@check
        val classSymbol = getClassOrObjectSymbolByClassId(classId) ?: return@check
        if (classSymbol !is KtNamedClassOrObjectSymbol) return@check
        if (classSymbol.isInline) return ObjCIdType
    }

    /* 'Irregular' inline class: Not marked as inline, but special K/N type that still gets inlined  */
    fullyExpandedType.getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        return inlineTargetType.mapToReferenceTypeIgnoringNullability()
    }

    /**
     * Simplified version of [org.jetbrains.kotlin.backend.konan.objcexport.CustomTypeMapper]
     */
    val typesMap = mutableMapOf<ClassId, String>().apply {
        this[ClassId.topLevel(StandardNames.FqNames.list)] = "NSArray"
        this[ClassId.topLevel(StandardNames.FqNames.mutableList)] = "NSMutableArray"
        this[ClassId.topLevel(StandardNames.FqNames.set)] = "NSSet"
        this[ClassId.topLevel(StandardNames.FqNames.mutableSet)] = "MutableSet".getObjCKotlinStdlibClassOrProtocolName().objCName
        this[ClassId.topLevel(StandardNames.FqNames.map)] = "NSDictionary"
        this[ClassId.topLevel(StandardNames.FqNames.mutableMap)] = "MutableDictionary".getObjCKotlinStdlibClassOrProtocolName().objCName
        this[ClassId.topLevel(StandardNames.FqNames.string.toSafe())] = "NSString"
    }

    NSNumberKind.entries.forEach { number ->
        val numberClassId = number.mappedKotlinClassId
        if (numberClassId != null) {
            typesMap[numberClassId] = numberClassId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName
        }
    }

    if (fullyExpandedType is KtNonErrorClassType) {
        val typeName = typesMap[classId]
            ?: fullyExpandedType.classId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName

        val typeArguments = translateToObjCTypeArguments()
        return ObjCClassType(typeName, typeArguments)
    }

    if (fullyExpandedType is KtTypeParameterType) {
        if (fullyExpandedType.symbol.getContainingSymbol() is KtCallableSymbol) {
            return ObjCIdType
        }
        /*
        Todo: K1 has some name mangling logic here?
        */
        return ObjCGenericTypeParameterUsage(fullyExpandedType.name.asString().toValidObjCSwiftIdentifier())
    }

    /* We cannot translate this, lets try to be lenient and emit the error type? */
    return objCErrorType
}


context(KtAnalysisSession, KtObjCExportSession)
private fun KtType.translateToObjCTypeArguments(): List<ObjCNonNullReferenceType> {
    if (this !is KtNonErrorClassType) return emptyList()

    /* See special casing below */
    val isKnownCollectionType = classId in collectionClassIds

    return ownTypeArguments.map { typeArgument ->
        when (typeArgument) {
            is KtStarTypeProjection -> ObjCIdType
            is KtTypeArgumentWithVariance -> {
                /*
                Kotlin `null` keys and values are represented as `NSNull` singleton in collections
                */
                if (isKnownCollectionType && typeArgument.type.isMarkedNullable) return@map ObjCIdType
                typeArgument.type.mapToReferenceTypeIgnoringNullability()
            }
        }
    }
}

context(KtAnalysisSession)
private fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KtType): ObjCReferenceType {
    return if (kotlinType.isBinaryRepresentationNullable()) {
        ObjCNullableReferenceType(this)
    } else {
        this
    }
}

context(KtAnalysisSession)
private fun KtType.isBinaryRepresentationNullable(): Boolean {
    /* Convention to match K1 implementation */
    if (this is KtErrorType) return false

    if (fullyExpandedType.canBeNull) return true

    getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        if (inlineTargetType.canBeNull) return true
    }

    return false
}


/**
 * Types to be "hidden" during mapping, i.e. represented as `id`.
 *
 * Currently contains super types of classes handled by custom type mappers.
 * Note: can be generated programmatically, but requires stdlib in this case.
 */
private val hiddenClassIds: Set<ClassId> = listOf(
    "kotlin.Any",
    "kotlin.CharSequence",
    "kotlin.Comparable",
    "kotlin.Function",
    "kotlin.Number",
    "kotlin.collections.Collection",
    "kotlin.collections.Iterable",
    "kotlin.collections.MutableCollection",
    "kotlin.collections.MutableIterable"
).map { ClassId.topLevel(FqName(it)) }.toSet()

private val collectionClassIds = setOf(
    StandardClassIds.List, StandardClassIds.MutableList,
    StandardClassIds.Set, StandardClassIds.MutableSet,
    StandardClassIds.Map, StandardClassIds.MutableMap
)