/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.withKtObjCExportSession
import org.jetbrains.kotlin.psi.KtElement

inline fun <T> analyzeWithObjCExport(
    useSiteKtElement: KtElement,
    configuration: KtObjCExportConfiguration = KtObjCExportConfiguration(),
    action: ObjCExportContext.() -> T,
): T = analyze(useSiteKtElement) {
    val kaSession: KaSession = this
    withKtObjCExportSession(configuration) {
        val exportSession = this
        with(ObjCExportContext(kaSession, exportSession)) {
            action.invoke(this)
        }
    }
}
