package org.jetbrains.kotlin.objcexport.analysisApiUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.psi.KtFile

context(KtAnalysisSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KtFileSymbol.getFileName(): String? {
    val ktFile = this.psi as? KtFile ?: return null
    return NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(ktFile.name))
}