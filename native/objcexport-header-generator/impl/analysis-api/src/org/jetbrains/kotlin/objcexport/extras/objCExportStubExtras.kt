package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal data class ObjCExportStubExtrasBuilderContext(val extras: MutableExtras)

/**
 * Convenience function for building extras for [ObjCExportStub]
 */
internal fun objCExportStubExtras(builder: MutableExtras.() -> Unit): Extras {
    return mutableExtrasOf().also { extras -> builder(extras) }
}