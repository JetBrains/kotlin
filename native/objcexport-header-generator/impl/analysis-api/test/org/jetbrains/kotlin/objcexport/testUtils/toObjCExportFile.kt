package org.jetbrains.kotlin.objcexport.testUtils

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.*

@Language("kotlin")
internal fun String.toObjCExportFile(
    inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
    run: ObjCExportContext.(KtResolvedObjCExportFile) -> Unit,
) {
    val file = inlineSourceCodeAnalysis.createKtFile(this)
    analyze(file) {
        val kaSession = this
        withKtObjCExportSession(KtObjCExportConfiguration()) {
            with(ObjCExportContext(analysisSession = kaSession, exportSession = this)) {
                run(with(KtObjCExportFile(file)) {
                    kaSession.resolve()
                })
            }
        }
    }
}