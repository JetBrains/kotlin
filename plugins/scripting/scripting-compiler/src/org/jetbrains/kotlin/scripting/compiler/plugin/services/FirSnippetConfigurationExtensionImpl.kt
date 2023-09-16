/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.FirScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.FirSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.builder.FirFileBuilder
import org.jetbrains.kotlin.fir.declarations.builder.FirSnippetBuilder
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.snippetScopesConfigurators
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ReplState
import kotlin.script.experimental.host.ScriptingHostConfiguration

class FirSnippetConfigurationExtensionImpl(
    session: FirSession,
    val replState: ReplState,
): FirSnippetConfiguratorExtension(session) {
    override fun FirSnippetBuilder.configure(sourceFile: KtSourceFile) {
        TODO("Not yet implemented")
    }

    override fun FirSnippetBuilder.configureContainingFile(fileBuilder: FirFileBuilder) {
        fileBuilder.packageDirective = buildPackageDirective {
            // TODO: generate reliable names
            val directive = replState.name.child(Name.identifier(fileBuilder.name))

            this.packageFqName = directive

            session.extensionService.snippetScopesConfigurators
        }
    }

    companion object {
        fun getFactory(replState: ReplState): Factory {
            return Factory { session -> FirSnippetConfigurationExtensionImpl(session, replState) }
        }
    }
}
