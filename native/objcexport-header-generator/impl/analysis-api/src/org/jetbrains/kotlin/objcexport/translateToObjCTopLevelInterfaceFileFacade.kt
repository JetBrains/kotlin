/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl
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
 */
context(KtAnalysisSession, KtObjCExportSession)
fun KtFileSymbol.translateToObjCTopLevelInterfaceFileFacade(): ObjCInterface? {
    val topLevelCallableStubs = getFileScope().getCallableSymbols()
        .sortedWith(StableCallableOrder)
        .mapNotNull { callableSymbol -> callableSymbol.translateToObjCExportStub() }
        .toList()
        /* If there are no top level functions or properties, we do not need to export a file facade */
        .ifEmpty { return null }

    val fileName = getObjCFileClassOrProtocolName()
        ?: throw IllegalStateException("Top level file '$this' cannot be translated without file name")

    val name = fileName.objCName
    val attributes = listOf(OBJC_SUBCLASSING_RESTRICTED)

    val superClass = getDefaultSuperClassOrProtocolName()

    return ObjCInterfaceImpl(
        name = name,
        comment = null,
        origin = null,
        attributes = attributes,
        superProtocols = emptyList(),
        members = topLevelCallableStubs,
        categoryName = null,
        generics = emptyList(),
        superClass = superClass.objCName,
        superClassGenerics = emptyList()
    )
}
