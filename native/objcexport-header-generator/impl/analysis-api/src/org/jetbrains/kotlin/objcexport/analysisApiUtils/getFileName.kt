package org.jetbrains.kotlin.objcexport.analysisApiUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.psi.KtFile

context(KaSession, KtObjCExportSession)
internal fun KaFileSymbol.getFileName(): String? {
    val ktFile = this.psi as? KtFile ?: return null
    return NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(ktFile.name))
}