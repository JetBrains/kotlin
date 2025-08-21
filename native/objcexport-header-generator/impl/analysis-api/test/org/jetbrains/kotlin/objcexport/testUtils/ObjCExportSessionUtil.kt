/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.KtObjCExportConfiguration
import org.jetbrains.kotlin.objcexport.KtObjCExportFile
import org.jetbrains.kotlin.objcexport.KtResolvedObjCExportFile
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

fun InlineSourceCodeAnalysis.createObjCExportFile(@Language("kotlin") sourceCode: String, run: ObjCExportContext.(KtResolvedObjCExportFile) -> Unit) {
    val ktFile = createKtFile(sourceCode)
    analyze(ktFile) {
        val kaSession = this
        withKtObjCExportSession(KtObjCExportConfiguration()) {
            with(ObjCExportContext(analysisSession = kaSession, exportSession = this)) {
                run(analyze(ktFile) { with(KtObjCExportFile(ktFile)) { resolve() } })
            }
        }
    }
}