/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStubOrigin
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getKDocString

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStubFactoriesKt.ObjCExportStubOrigin]
 */
fun KaSession.getObjCExportStubOrigin(symbol: KaSymbol): ObjCExportStubOrigin {
    // TODO: Differentiate origins
    // TODO: Extract kdoc from deserialized symbols
    return ObjCExportStubOrigin.Source(
        name = symbol.name,
        psi = symbol.psi,
        kdoc = symbol.getKDocString()
    )
}
