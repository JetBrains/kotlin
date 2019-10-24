/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.extensions

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportInfo
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.extensions.ExtraImportsProviderExtension
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class ScriptExtraImportsProviderExtension : ExtraImportsProviderExtension {

    // initially copied from org.jetbrains.kotlin.resolve.lazy.FileScopeFactory.DefaultImportImpl, but kept separate to simplify dependencies
    // and allow easier extension
    private class ScriptExtraImportImpl(private val importPath: ImportPath) : KtImportInfo {
        override val isAllUnder: Boolean get() = importPath.isAllUnder

        override val importContent = KtImportInfo.ImportContent.FqNameBased(importPath.fqName)

        override val aliasName: String? get() = importPath.alias?.asString()

        override val importedFqName: FqName? get() = importPath.fqName
    }

    override fun getExtraImports(ktFile: KtFile): Collection<KtImportInfo> =
        ktFile.takeIf { runReadAction { it.isScript() } }?.let { file ->
            val refinedConfiguration = ScriptDependenciesProvider.getInstance(file.project)
                ?.getScriptConfiguration(file.originalFile as KtFile)
            refinedConfiguration?.defaultImports?.map {
                ScriptExtraImportImpl(
                    ImportPath.fromString(it)
                )
            }
        }.orEmpty()
}