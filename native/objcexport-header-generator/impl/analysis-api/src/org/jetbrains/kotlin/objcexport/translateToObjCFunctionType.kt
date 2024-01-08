@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.BlockPointerBridge
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCReferenceType

internal fun KtType.translateToObjCFunctionType(typeBridge: BlockPointerBridge): ObjCReferenceType {
    TODO()
}
