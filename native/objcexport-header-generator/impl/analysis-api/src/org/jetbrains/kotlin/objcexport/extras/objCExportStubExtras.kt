package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal object ObjCExportStubExtrasBuilderContext

/**
 * Convenience function for building extras for [ObjCExportStub]
 */
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun objCExportStubExtras(builder: context(ObjCExportStubExtrasBuilderContext) MutableExtras.() -> Unit): Extras {
    return mutableExtrasOf().also { extras -> builder(ObjCExportStubExtrasBuilderContext, extras) }
}