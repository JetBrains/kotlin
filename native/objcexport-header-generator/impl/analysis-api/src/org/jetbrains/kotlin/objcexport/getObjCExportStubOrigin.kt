/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossiblyNamedSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStubOrigin
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getKDocString

context(KtAnalysisSession)
fun KtSymbol.getObjCExportStubOrigin(): ObjCExportStubOrigin {
    // TODO: Differentiate origins
    // TODO: Extract kdoc from deserialized symbols
    return ObjCExportStubOrigin.Source(
        name = let { it as? KtPossiblyNamedSymbol }?.name,
        psi = psi,
        kdoc = getKDocString()
    )
}
