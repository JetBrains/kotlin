package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

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

    NSNumberKind.entries.forEach {
        val classId = it.mappedKotlinClassId
        if (classId != null) {
            typesMap[classId] = classId.shortClassName.asString().getObjCKotlinStdlibClassOrProtocolName().objCName
        }
    }

    val typeName = typesMap[this.expandedClassSymbol?.classIdIfNonLocal]
        ?: throw IllegalStateException("Unsupported mapping type for $this")

    return ObjCClassType(typeName)
}

private fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KtType): ObjCReferenceType {
    return if (kotlinType.nullability.isNullable) {
        ObjCNullableReferenceType(this)
    } else {
        this
    }
}
