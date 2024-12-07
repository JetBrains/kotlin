/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrScriptConfiguratorExtension
import org.jetbrains.kotlin.fir.backend.Fir2IrScriptConfiguratorExtension.Factory
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirScriptSymbol
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.host.ScriptingHostConfiguration

class Fir2IrScriptConfiguratorExtensionImpl(
    session: FirSession,
    @Suppress("UNUSED_PARAMETER") hostConfiguration: ScriptingHostConfiguration
) : Fir2IrScriptConfiguratorExtension(session) {
    override fun IrScript.configure(script: FirScript, getIrScriptByFirSymbol: (FirScriptSymbol) -> IrScriptSymbol?) {
        // processing only refined scripts here
        val scriptFile = session.firProvider.getFirScriptContainerFile(script.symbol) ?: return
        val scriptSourceFile = scriptFile.sourceFile?.toSourceCode() ?: return
        val compilationConfiguration = session.getScriptCompilationConfiguration(scriptSourceFile, getDefault = { null }) ?: return

        // assuming that if the script is compiled, the import files should be all resolved already
        val importedScripts = compilationConfiguration[ScriptCompilationConfiguration.resolvedImportScripts]?.takeIf { it.isNotEmpty() } ?: return
        val importedScriptSymbols = importedScripts.mapNotNull {
            session.firProvider.getFirScriptByFilePath(it.locationId!!)  // TODO: all !! should be converted to diagnostics
        }

        this.importedScripts = importedScriptSymbols.map { getIrScriptByFirSymbol(it)!! }.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> Fir2IrScriptConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}