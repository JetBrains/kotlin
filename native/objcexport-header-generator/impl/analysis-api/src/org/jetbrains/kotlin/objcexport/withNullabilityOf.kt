package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtErrorType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCReferenceType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull

/**
 * [ObjCNonNullReferenceType] must be converted into [ObjCNullableReferenceType] if type is nullable
 * So types could be marked with "_Nullable" in Objective-C
 */
context(KtAnalysisSession)
internal fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KtType): ObjCReferenceType {
    return if (kotlinType.isBinaryRepresentationNullable()) {
        ObjCNullableReferenceType(this)
    } else {
        this
    }
}

context(KtAnalysisSession)
internal fun KtType.isBinaryRepresentationNullable(): Boolean {
    /* Convention to match K1 implementation */
    if (this is KtErrorType) return false

    if (fullyExpandedType.canBeNull) return true

    getInlineTargetTypeOrNull()?.let { inlineTargetType ->
        if (inlineTargetType.canBeNull) return true
    }

    return false
}