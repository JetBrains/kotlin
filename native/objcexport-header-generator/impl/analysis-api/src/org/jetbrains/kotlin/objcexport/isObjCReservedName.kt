package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.cKeywords

/**
 * K1: See implementations of [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.Mapping.reserved]
 */
private val reservedPropertyNames = cKeywords + setOf("description") // https://youtrack.jetbrains.com/issue/KT-38641

/**
 * Following class and object names should be handled in a special way to avoid clashing with NSObject class methods.
 *
 * When processing ["alloc", "copy", "mutableCopy", "new", "init"] names the `get` prefix should be added.
 * Other reserved names are mangled by adding `_` suffix.
 */
private val reservedClassOrObjectNames = setOf(
    "retain", "release", "autorelease",
    "initialize", "load", "alloc", "new", "class", "superclass",
    "classFallbacksForKeyedArchiver", "classForKeyedUnarchiver",
    "description", "debugDescription", "version", "hash",
    "useStoredAccessor"
) + cKeywords

internal val String.isReservedPropertyName: Boolean
    get() = this in reservedPropertyNames

private val String.isReservedClassOrObjectName: Boolean
    get() = this in reservedClassOrObjectNames

/**
 * There are set of reserved names for classes, objects, methods and properties which should not be translated as they are,
 * but translated with a suffix `_`.
 *
 * For example `bool` isn't Objective-C type, but if it's used in header as property or class name invalid header would be generated
 * because `bool` is defined in `clang/stdbool.h`
 *
 * See [reservedClassOrObjectNames], [reservedPropertyNames] and [reservedPropertyNames]
 */
internal fun String.mangleIfReservedObjCName(): String = if (this.isReservedClassOrObjectName) this + "_" else this