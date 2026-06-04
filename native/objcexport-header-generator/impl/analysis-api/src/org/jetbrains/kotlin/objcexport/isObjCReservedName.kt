package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.cKeywords
import org.jetbrains.kotlin.backend.konan.objCMacroDefinitions
import org.jetbrains.kotlin.backend.konan.reservedObjCClassOrObjectNames

/**
 * K1: See implementations of [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.Mapping.reserved]
 */
private val reservedPropertyNames = cKeywords + setOf("description") + objCMacroDefinitions // https://youtrack.jetbrains.com/issue/KT-38641

internal val String.isReservedPropertyName: Boolean
    get() = this in reservedPropertyNames

private val String.isReservedClassOrObjectName: Boolean
    get() = this in reservedObjCClassOrObjectNames

/**
 * There are set of reserved names for classes, objects, methods and properties which should not be translated as they are,
 * but translated with a suffix `_`.
 *
 * For example `bool` isn't Objective-C type, but if it's used in header as property or class name invalid header would be generated
 * because `bool` is defined in `clang/stdbool.h`
 *
 * See [reservedObjCClassOrObjectNames], [reservedPropertyNames] and [reservedPropertyNames]
 */
internal fun String.mangleIfReservedObjCName(): String = if (this.isReservedClassOrObjectName) this + "_" else this