/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.extensions.FirScriptResolutionConfigurationExtension
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.scripting.compiler.plugin.fir.scriptCompilationConfiguration
import org.jetbrains.kotlin.scripting.resolve.toSourceCode
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.host.ScriptingHostConfiguration

class FirScriptResolutionConfigurationExtensionImpl(
    session: FirSession,
) : FirScriptResolutionConfigurationExtension(session) {

    override fun getScriptDefaultImports(script: FirScript): List<FirImport>? =
        getDefaultImports(script, script.scriptCompilationConfiguration) {
            script.moduleData.session.firProvider.getFirScriptContainerFile(script.symbol)
        }

    override fun getSnippetDefaultImports(snippet: FirReplSnippet): List<FirImport>? =
        getDefaultImports(snippet, snippet.scriptCompilationConfiguration) {
            snippet.moduleData.session.firProvider.getFirReplSnippetContainerFile(snippet.symbol)
        }

    private inline fun getDefaultImports(
        declaration: FirDeclaration,
        attachedConfiguration: ScriptCompilationConfiguration?,
        getContainingFile: () -> FirFile?,
    ): List<FirImport>? {
        val compilationConfiguration = attachedConfiguration ?: run {
            val sourceCode = getContainingFile()?.sourceFile?.toSourceCode() ?: return emptyList()
            @Suppress("DEPRECATION")
            session.getScriptCompilationConfiguration(sourceCode, getDefault = { null })
        } ?: return emptyList()

        return compilationConfiguration[ScriptCompilationConfiguration.defaultImports]
            .firImportsFromDefaultImports(declaration.source?.fakeElement(KtFakeSourceElementKind.ImplicitImport))
    }

    companion object {
        fun getFactory(): Factory {
            return Factory { session -> FirScriptResolutionConfigurationExtensionImpl(session) }
        }

        @Deprecated("Use other getFactory methods. This one left only for transitional compatibility")
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirScriptResolutionConfigurationExtensionImpl(session) }
        }
    }
}

internal fun List<String>?.firImportsFromDefaultImports(sourceElement: KtSourceElement?): List<FirImport>? =
    this?.map { defaultImport ->
        val trimmed = defaultImport.trim()
        val endsWithStar = trimmed.endsWith("*")
        val stripped = if (endsWithStar) trimmed.substring(0, trimmed.length - 2) else trimmed
        val fqName = FqName.fromSegments(stripped.split("."))
        buildImport {
            source = sourceElement
            importedFqName = fqName
            isAllUnder = endsWithStar
        }
    }
