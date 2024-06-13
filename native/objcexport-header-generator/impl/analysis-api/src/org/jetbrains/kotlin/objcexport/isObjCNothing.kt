package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Unlike AA, descriptors do verify nullability [org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNothing]
 * See K1 usage [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper.bridgeReturnType]
 */
context(KtAnalysisSession)
val KaType.isObjCNothing: Boolean
    get() = isNothing && !canBeNull