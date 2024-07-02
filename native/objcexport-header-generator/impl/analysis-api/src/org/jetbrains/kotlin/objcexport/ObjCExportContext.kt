package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession

/**
 * Solution for migration from context receivers to context parameters, until the latter aren't available
 */
class ObjCExportContext(
    val analysisSession: KaSession,
    val exportSession: KtObjCExportSession,
)