package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel

/**
 * We wrap kotlin extensions into interfaces with category `extensions`
 * ```objective-c
 * @interface Foo
 * @end
 * @interface Foo (extensions)
 * @end
 * ```
 * Order of extensions must comply with the following requirements:
 * 1. Extensions must be defined after extended interface, otherwise header is invalid and can't be compiled
 * 2. Extension might be defined without extended interface, in this case we need to keep it at the same index
 *
 * How reordering works
 * 1. Filter stubs which require reordering
 * 2. Insert them in correct order
 * 3. Insert remaining stubs at the same index
 *
 * How K1 works
 * See [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.translateExtensions]
 * In the beginning it calls `generateExtraClassEarly` which generates extended interface
 */
fun List<ObjCTopLevel>.reorderExtensionsIfNeeded(): List<ObjCTopLevel> {
    if (isEmpty()) return this
    val result = mutableListOf<ObjCTopLevel>()
    val stubs = mutableMapOf<String, ObjCTopLevel>()
    val toBeReordered = mutableMapOf<String, ObjCInterface>()

    this.forEach { stub ->
        if (stub.isExtension) {
            // If stub is extension, but not defined before we need to reorder it
            if (!stubs.containsKey(stub.name)) toBeReordered[stub.name] = stub as ObjCInterface
        } else stubs[stub.name] = stub
    }

    this.forEach { stub ->
        if (stub.isExtension) {
            // Categorized stub is in valid order
            if (!toBeReordered.containsKey(stub.name)) result.add(stub)
        } else {
            val stubForReorder = toBeReordered.remove(stub.name)
            if (stubForReorder == null) result.add(stub)
            else {
                result.add(stub)
                result.add(stubForReorder)
            }
        }
    }

    // Edge case for remaining stubs which don't have defining interface
    // To comply with K1 order we need to insert them at the same index
    toBeReordered.values.forEach { e ->
        result.add(indexOfFirst { stub -> stub == e }, e)
    }

    return result
}

private val ObjCTopLevel.isExtension: Boolean
    get() {
        return this is ObjCInterface && this.categoryName != null
    }