/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.builder.FirReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import kotlin.script.experimental.api.ReplScriptingHostConfigurationKeys
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The interface to an evaluation context getter
 */
typealias IsReplSnippetSourcePredicate = (KtSourceFile?, KtSourceElement) -> Boolean

/**
 * Predicate to use to distinguish REPL snippets from regular scripts. Not optional - should be provided by the REPL implementation
 */
val ReplScriptingHostConfigurationKeys.isReplSnippetSource by PropertiesCollection.key<IsReplSnippetSourcePredicate>(isTransient = true)

class FirReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    private val hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetConfiguratorExtension(session) {

    override fun isReplSnippetsSource(sourceFile: KtSourceFile?, scriptSource: KtSourceElement): Boolean =
        hostConfiguration[ScriptingHostConfiguration.repl.isReplSnippetSource]?.invoke(sourceFile, scriptSource) ?: false

    override fun FirReplSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
    }

    override fun FirReplSnippetBuilder.configure(sourceFile: KtSourceFile?, context: Context<PsiElement>) {
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}