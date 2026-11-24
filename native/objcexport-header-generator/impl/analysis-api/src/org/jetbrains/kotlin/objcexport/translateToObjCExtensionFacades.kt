/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl

private const val extensionsCategoryName = "Extensions"

/**
 * We iterate over all files to collect all extensions and group them by type symbol [KaClassSymbol]
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
internal fun ObjCExportContext.translateToObjCExtensionFacades(files: List<KtObjCExportFile>): Map<KaClassSymbol, ObjCClass> {
    return files
        .flatMap { file -> translateToObjCExtensionFacades(file.resolve(session = analysisSession)).entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { [_, facades] ->
            mergeExtensionFacades(facades.first().name, facades) //all facades has the same name, so just pick first one
        }
}

/**
 * Translates extension functions/properties inside the given [this] file as a single [ObjCInterface]
 * with category [extensionsCategoryName]
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
private fun ObjCExportContext.translateToObjCExtensionFacades(file: KtResolvedObjCExportFile): Map<KaClassSymbol, ObjCClass> {
    val extensions = file.callableSymbols
        .filter { getClassIfCategory(it, session = analysisSession) != null && it.isExtension }
        .sortedWith(getStableCallableOrder(session = analysisSession))
        .groupBy {
            val type = it.receiverParameter?.returnType
            if (isMappedObjCType(type, session = analysisSession)) return@groupBy null
            else {
                /**
                 * Mapped types extensions should be handled as top level facades
                 * @see [translateToObjCTopLevelFacade]
                 * @see [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper.getClassIfCategory] which K1 uses
                 * to differentiate extensions and top level callables
                 */
                val expandedSymbol = context(analysisSession) { type?.expandedSymbol }
                if (expandedSymbol == null) return@groupBy null
                else getObjCClassOrProtocolName(expandedSymbol).objCName
            }
        }.mapNotNull { [name, symbols] -> if (name == null) null else name to symbols }

    return extensions.mapNotNull { [objCName, extensionSymbols] ->
        val extensionType = extensionSymbols.map { it.receiverParameter?.returnType }.firstNotNullOf { it?.symbol as? KaClassSymbol }
        val translatedMembers = extensionSymbols.flatMap { ext -> translateToObjCExportStub(ext) }
        if (translatedMembers.isEmpty()) return@mapNotNull null
        extensionType to buildExtensionFacade(objCName, translatedMembers)
    }.toMap()
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

private fun mergeExtensionFacades(name: String, facades: List<ObjCClass>): ObjCClass {
    return buildExtensionFacade(name, facades.flatMap { f -> f.members })
}
