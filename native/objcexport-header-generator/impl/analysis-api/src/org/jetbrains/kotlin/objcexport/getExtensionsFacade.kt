package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFileName

private const val extensionsCategoryName = "Extensions"

internal val ObjCInterface.isExtensionsFacade: Boolean
    get() = this.categoryName == extensionsCategoryName

/**
 * Translates extension functions/properties inside the given [this] file as a single [ObjCInterface]
 * with category [extensionsCategoryName]
 *
 * Later interface should be forwarded using [isExtensionsFacade]
 *
 * ## example:
 * given a file "Foo.kt"
 *
 * ```kotlin
 *
 * fun Foo.func() = 42
 *
 * val Foo.prop get() = 42
 *
 * class Foo {
 *
 * }
 *
 * ```
 *
 * This will be exporting two Interfaces with forwarded class:
 *
 * ```
 * @class Foo
 *
 * @interface Foo: Base
 *
 * @interface Foo (Extensions)
 *      - func
 *      - prop
 * ```
 *
 * Where `Foo` would be the "top level interface file extensions facade" returned by this function.
 *
 * See related [getTopLevelFacade]
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFileSymbol.getExtensionsFacade(): ObjCInterface? {

    val extensions = getFileScope()
        .getCallableSymbols().filter { it.isExtension }
        .toList().sortedWith(StableCallableOrder)
        .ifEmpty { return null }

    val fileName = getFileName()
        ?: throw IllegalStateException("File '$this' cannot be translated without file name")

    return ObjCInterfaceImpl(
        name = fileName,
        comment = null,
        origin = null,
        attributes = emptyList(),
        superProtocols = emptyList(),
        members = extensions.mapNotNull { it.translateToObjCExportStub() },
        categoryName = extensionsCategoryName,
        generics = emptyList(),
        superClass = null,
        superClassGenerics = emptyList()
    )
}