package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isError
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType

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

    val classId = this.expandedClassSymbol?.classIdIfNonLocal
    val isInlined = false //TODO: replace when KT-65176 is implemented
    val isHidden = classId in hiddenTypes

    return if (isError) {
        objCErrorType
    } else if (isAny || isHidden || isInlined) {
        ObjCIdType
    } else {
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

        val typeName = typesMap[classId]
            ?: classId!!.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName

        val typeArguments = run {
            if (this !is KtNonErrorClassType) {
                return@run listOf<ObjCNonNullReferenceType>()
            }

            ownTypeArguments.mapNotNull { typeArgument -> typeArgument.type }
                .map { typeArgumentType -> typeArgumentType.mapToReferenceTypeIgnoringNullability() }
        }

        ObjCClassType(typeName, typeArguments)
    }
}

private fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KtType): ObjCReferenceType {
    return if (kotlinType.nullability.isNullable) {
        ObjCNullableReferenceType(this)
    } else {
        this
    }
}

/**
 * Types to be "hidden" during mapping, i.e. represented as `id`.
 *
 * Currently contains super types of classes handled by custom type mappers.
 * Note: can be generated programmatically, but requires stdlib in this case.
 */
private val hiddenTypes: Set<ClassId> = listOf(
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