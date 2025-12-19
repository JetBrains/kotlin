/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.objcexport.KtObjCExportFile
import org.jetbrains.kotlin.objcexport.KtResolvedObjCExportFile
import org.jetbrains.kotlin.objcexport.ObjCExportContext


internal fun ObjCExportContext.getFileContainingModule(file: KtObjCExportFile): KaModule? {
    val resolvedFile = with(file) { analysisSession.resolve() }
    return getFileContainingModule(resolvedFile)
}

internal fun ObjCExportContext.getFileContainingModule(resolvedFile: KtResolvedObjCExportFile): KaModule? {
    return with(analysisSession) {
        resolvedFile.file?.symbol?.containingModule
    }
}