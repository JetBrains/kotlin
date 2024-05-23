package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.cKeywords

/**
 * K1: See implementations of [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.Mapping.reserved]
 */
private val reservedPropertyNames = cKeywords + setOf("description") // https://youtrack.jetbrains.com/issue/KT-38641
private val reservedClassNames = setOf(
    "retain", "release", "autorelease",
    "initialize", "load", "alloc", "new", "class", "superclass",
    "classFallbacksForKeyedArchiver", "classForKeyedUnarchiver",
    "description", "debugDescription", "version", "hash",
    "useStoredAccessor"
) + cKeywords

internal val String.isReservedPropertyName: Boolean
    get() = this in reservedPropertyNames

internal val String.isReservedClassName: Boolean // See KT-68050
    get() = this in reservedClassNames

/**
 * There are set of reserved names for classes, methods and properties which should not be translated as they are,
 * but translated with a suffix `_`.
 *
 * For example `bool` isn't Objective-C type, but if it's used in header as property or class name invalid header would be generated
 * because `bool` is defined in `clang/stdbool.h`
 *
 * See [reservedClassNames], [reservedPropertyNames] and [reservedPropertyNames]
 */
internal fun String.mangleReservedObjCName(): String = this + "_"