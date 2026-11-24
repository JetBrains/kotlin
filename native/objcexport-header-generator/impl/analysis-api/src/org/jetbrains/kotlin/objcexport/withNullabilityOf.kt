/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.fullyExpandedType
import org.jetbrains.kotlin.analysis.api.types.isNullable
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCReferenceType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull

/**
 * [ObjCNonNullReferenceType] must be converted into [ObjCNullableReferenceType] if type is nullable
 * So types could be marked with "_Nullable" in Objective-C
 */
context(session: KaSession)
internal fun withNullabilityOf(objType: ObjCNonNullReferenceType, kotlinType: KaType): ObjCReferenceType {
    return if (isBinaryRepresentationNullable(kotlinType)) {
        ObjCNullableReferenceType(objType)
    } else {
        objType
    }
}

context(session: KaSession)
internal fun isBinaryRepresentationNullable(type: KaType): Boolean {
    /* Convention to match K1 implementation */
    if (type is KaErrorType) return false

    if (type.fullyExpandedType.isNullable) return true

    getInlineTargetTypeOrNull(type)?.let { inlineTargetType ->
        if (inlineTargetType.isNullable) return true
    }

    return false
}
