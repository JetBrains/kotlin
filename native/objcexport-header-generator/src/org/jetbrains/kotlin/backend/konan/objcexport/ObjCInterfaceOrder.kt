package org.jetbrains.kotlin.backend.konan.objcexport

/**
 * We wrap kotlin extensions into interfaces with category `extensions`
 * ```objective-c
 * @interface Foo
 * @end
 * @interface Foo (extensions)
 * @end
 * ```
 * Interfaces with category must be defined after extended interface, otherwise header is invalid and can't be compiled.
 * This comparator shifts all categorized interfaces to last positions.
 *
 * How K1 works
 * See [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.translateExtensions]
 * In the beginning it calls `generateExtraClassEarly` which generates extended interface
 */
val ObjCInterfaceOrder: Comparator<ObjCExportStub> = Comparator { stub1, stub2 ->
    if (stub1 is ObjCInterface && stub1.categoryName != null) 1
    else if (stub2 is ObjCInterface && stub2.categoryName != null) -1
    else 0
}