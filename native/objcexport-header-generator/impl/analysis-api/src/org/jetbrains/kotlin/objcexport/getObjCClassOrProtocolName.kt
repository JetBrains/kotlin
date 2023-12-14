/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.nameOrAnonymous
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassLikeSymbol.getObjCClassOrProtocolName(): ObjCExportClassOrProtocolName {
    val resolvedObjCNameAnnotation = resolveObjCNameAnnotation()

    return ObjCExportClassOrProtocolName(
        objCName = resolvedObjCNameAnnotation?.objCName ?: nameOrAnonymous.asString(),
        swiftName = resolvedObjCNameAnnotation?.swiftName ?: nameOrAnonymous.asString()
    )
}