package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl

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
 * ## What function returns
 * Function returns extended type(s) and extension facade(s).
 * If extended type is translated already it will be filtered out by [KtObjCExportHeaderGenerator]
 * See [org.jetbrains.kotlin.objcexport.KtObjCExportHeaderGenerator.addObjCStubIfNotTranslated]
 *
 * See related [translateToObjCTopLevelFacade]
 */
internal fun ObjCExportContext.translateToObjCExtensionFacades(file: KtResolvedObjCExportFile): List<ObjCClass> {
    val extensions = file.callableSymbols
        .filter { analysisSession.getClassIfCategory(it) != null && it.isExtension }
        .sortedWith(StableCallableOrder)
        .groupBy {
            val type = it.receiverParameter?.returnType
            if (analysisSession.isMappedObjCType(type)) return@groupBy null
            else {
                /**
                 * Mapped types extensions should be handled as top level facades
                 * @see [translateToObjCTopLevelFacade] and [isExtensionOfMappedObjCType]
                 * @see [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper.getClassIfCategory] which K1 uses
                 * to differentiate extensions and top level callables
                 */
                val expandedSymbol = with(analysisSession) { type?.expandedSymbol }
                if (expandedSymbol == null) return@groupBy null
                else getObjCClassOrProtocolName(expandedSymbol).objCName
            }
        }.mapNotNull { (name, symbols) -> if (name == null) null else name to symbols }

    return extensions.flatMap { (objCName, extensionSymbols) ->
        val extensionTypes = extensionSymbols
            .map { it.receiverParameter?.returnType }
            .mapNotNull { it?.symbol as? KaClassSymbol }
            .mapNotNull { translateToObjCClass(it) }

        val translatedMembers = extensionSymbols.flatMap { ext -> translateToObjCExportStub(ext) }

        if (translatedMembers.isEmpty()) {
            extensionTypes
        } else {
            extensionTypes + buildExtensionFacade(objCName, translatedMembers)
        }
    }
}

/**
 * We iterate over all files to collect all extensions and group them by type name.
 * And finally merge extensions into facades
 *
 * File `Foo.kt`:
 * ```kotlin
 * fun String.foo()
 * ```
 * File `Bar.kt`
 * ```kotlin
 * fun String.bar()
 * ```
 * Result facade
 * ```kotlin
 * interface StringExtensions {
 *  fun String.foo()
 *  fun String.bar()
 * }
 * ```
 */
internal fun ObjCExportContext.translateToObjCExtensionFacades(files: List<KtObjCExportFile>): List<ObjCClass> {

    val result = mutableListOf<ObjCClass>()

    val facadesMap = files.flatMap { file ->
        val resolvedFile = with(file) { analysisSession.resolve() }
        translateToObjCExtensionFacades(resolvedFile)
    }.groupBy { it.name }

    facadesMap.forEach {
        val name = it.key
        val facades = it.value
        val (extensions, extendedTypes) = facades.partition { f -> f.isExtensionFacade }

        result.add(mergeExtensionFacades(name, extensions))
        result.addAll(extendedTypes)
    }
    return result
}

private fun buildExtensionFacade(objCName: String, members: List<ObjCExportStub>): ObjCClass {
    return ObjCInterfaceImpl(
        name = objCName,
        comment = null,
        origin = null,
        attributes = emptyList(),
        superProtocols = emptyList(),
        members = members,
        categoryName = extensionsCategoryName,
        generics = emptyList(),
        superClass = null,
        superClassGenerics = emptyList()
    )
}

private val ObjCClass.isExtensionFacade: Boolean
    get() = this is ObjCInterface && this.categoryName == extensionsCategoryName

private fun mergeExtensionFacades(name: String, facades: List<ObjCClass>): ObjCClass {
    return buildExtensionFacade(name, facades.flatMap { f -> f.members })
}