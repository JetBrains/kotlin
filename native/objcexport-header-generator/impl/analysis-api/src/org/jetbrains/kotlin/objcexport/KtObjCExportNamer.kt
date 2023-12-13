/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportPropertyName

interface KtObjCExportNamer {
    context(KtAnalysisSession)
    fun getClassOrProtocolName(symbol: KtClassLikeSymbol): ObjCExportClassOrProtocolName

    context(KtAnalysisSession)
    fun getPropertyName(symbol: KtPropertySymbol): ObjCExportPropertyName
}

fun KtObjCExportNamer(): KtObjCExportNamer {
    return ObjCExportNamerImpl()
}

private class ObjCExportNamerImpl : KtObjCExportNamer {
    context(KtAnalysisSession)
    override fun getClassOrProtocolName(symbol: KtClassLikeSymbol): ObjCExportClassOrProtocolName {
        val resolvedObjCNameAnnotation = symbol.resolveObjCNameAnnotation()

        return ObjCExportClassOrProtocolName(
            objCName = resolvedObjCNameAnnotation?.objCName ?: symbol.nameOrAnonymous.asString(),
            swiftName = resolvedObjCNameAnnotation?.swiftName ?: symbol.nameOrAnonymous.asString()
        )
    }

    context(KtAnalysisSession)
    override fun getPropertyName(symbol: KtPropertySymbol): ObjCExportPropertyName {
        val resolveObjCNameAnnotation = symbol.resolveObjCNameAnnotation()

        return ObjCExportPropertyName(
            objCName = resolveObjCNameAnnotation?.objCName ?: symbol.name.asString(),
            swiftName = resolveObjCNameAnnotation?.swiftName ?: symbol.name.asString()
        )
    }
}
