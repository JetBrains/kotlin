/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl
import org.jetbrains.kotlin.backend.konan.objcexport.toNameAttributes
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName


/**
 * Translates top level functions/properties inside the given [this] file as a single [ObjCInterface].
 * ## example:
 * given a file "Foo.kt"
 *
 * ```kotlin
 *
 * fun myTopLevelFunction() = 42
 *
 * val myTopLevelProperty get() = 42
 *
 * class Bar {
 *      fun bar() = Unit
 * }
 *
 * ```
 *
 * This will be exporting two Interfaces:
 *
 * ```
 * FooKt: Base
 *      - myTopLevelFunction
 *      - myTopLevelProperty
 *
 * Bar
 *      - bar
 * ```
 *
 * Where `FooKt` would be the "top level interface file facade" returned by this function.
 *
 * See related [translateToObjCExtensionFacades]
 */
fun ObjCExportContext.translateToObjCTopLevelFacade(file: KtResolvedObjCExportFile): ObjCInterface? {
    val topLevelCallables = file.callableSymbols
        .filter { analysisSession.getClassIfCategory(it) == null }
        .toList()
        .sortedWith(analysisSession.getStableCallableOrder())
        .flatMap { translateToObjCExportStub(it) }

    val fileName = getObjCFileClassOrProtocolName(file)

    if (topLevelCallables.isNotEmpty()) return ObjCInterfaceImpl(
        name = fileName.objCName,
        comment = null,
        origin = null,
        attributes = listOf(OBJC_SUBCLASSING_RESTRICTED) + fileName.toNameAttributes(),
        superProtocols = emptyList(),
        members = topLevelCallables,
        categoryName = null,
        generics = emptyList(),
        superClass = exportSession.getDefaultSuperClassOrProtocolName().objCName,
        superClassGenerics = emptyList()
    )
    return null
}

internal fun ObjCExportContext.isExtensionOfMappedObjCType(symbol: KaCallableSymbol): Boolean {
    val receiverType = symbol.receiverParameter?.returnType
    return symbol.isExtension && receiverType != null && analysisSession.isMappedObjCType(receiverType)
}