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
    val isStub1ExtensionFacade = (stub1 as? ObjCInterface)?.categoryName != null
    val isStub2ExtensionFacade = (stub2 as? ObjCInterface)?.categoryName != null
    when {
        isStub1ExtensionFacade && isStub2ExtensionFacade -> stub1.name.compareTo(stub2.name)
        isStub1ExtensionFacade -> 1
        isStub2ExtensionFacade -> -1
        else -> 0
    }
}