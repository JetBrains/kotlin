package org.jetbrains.kotlin.objcexport.extras

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

private val throwsAnnotationClassIdsKey = extrasKeyOf<List<ClassId>?>()

/**
 * Functions and constructors can have [Throws] annotations which lists exceptions.
 * This lists of exceptions needs to be translated and forwarded as dependencies
 */
internal val ObjCExportStub.throwsAnnotationClassIds: List<ClassId>?
    get() {
        extras[throwsAnnotationClassIdsKey]?.let { return it }
        return null
    }

internal var MutableExtras.throwsAnnotationClassIds: List<ClassId>?
    get() = this[throwsAnnotationClassIdsKey]
    set(value) {
        this[throwsAnnotationClassIdsKey] = value
    }