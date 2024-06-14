package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCReferenceType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull

/**
 * [ObjCNonNullReferenceType] must be converted into [ObjCNullableReferenceType] if type is nullable
 * So types could be marked with "_Nullable" in Objective-C
 */
context(KaSession)
internal fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KaType): ObjCReferenceType {
    return if (kotlinType.isBinaryRepresentationNullable()) {
        ObjCNullableReferenceType(this)
    } else {
        this
    }
}

context(KaSession)
internal fun KaType.isBinaryRepresentationNullable(): Boolean {
    /* Convention to match K1 implementation */
    if (this is KaErrorType) return false

    if (fullyExpandedType.canBeNull) return true

    getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        if (inlineTargetType.canBeNull) return true
    }

    return false
}