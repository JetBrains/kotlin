package org.jetbrains.kotlin.objcexport.analysisApiUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.KtFile

internal fun getFileName(file: KaFileSymbol): String? {
    val ktFile = file.psi as? KtFile ?: return null
    return NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(ktFile.name))
}