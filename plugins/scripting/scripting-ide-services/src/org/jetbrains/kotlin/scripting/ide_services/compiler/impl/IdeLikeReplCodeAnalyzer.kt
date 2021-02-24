/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler.impl

import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities

class IdeLikeReplCodeAnalyzer(
    private val environment: KotlinCoreEnvironment,
    implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter
) : ReplCodeAnalyzerBase(environment, CliBindingTrace(), implicitsResolutionFilter) {
    interface ReplLineAnalysisResultWithStateless : ReplLineAnalysisResult {
        // Result of stateless analyse, which may be used for reporting errors
        // without code generation
        data class Stateless(
            override val diagnostics: Diagnostics,
            val bindingContext: BindingContext,
            val resolutionFacade: KotlinResolutionFacadeForRepl,
            val moduleDescriptor: ModuleDescriptor,
            val resultProperty: PropertyDescriptor?,
        ) :
            ReplLineAnalysisResultWithStateless {
            override val scriptDescriptor: ClassDescriptorWithResolutionScopes? get() = null
        }
    }

    fun statelessAnalyzeWithImportedScripts(
        psiFile: KtFile,
        importedScripts: List<KtFile>,
        priority: Int
    ): ReplLineAnalysisResultWithStateless {
        topDownAnalysisContext.scripts.clear()
        trace.clearDiagnostics()

        psiFile.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, priority)

        return doStatelessAnalyze(psiFile, importedScripts)
    }

    private fun doStatelessAnalyze(linePsi: KtFile, importedScripts: List<KtFile>): ReplLineAnalysisResultWithStateless {
        scriptDeclarationFactory.setDelegateFactory(
            FileBasedDeclarationProviderFactory(resolveSession.storageManager, listOf(linePsi) + importedScripts)
        )
        replState.submitLine(linePsi)

        val context = runAnalyzer(linePsi, importedScripts)

        val resultPropertyDescriptor = (context.scripts[linePsi.script] as? ScriptDescriptor)?.resultValue

        val moduleDescriptor = container.getService(ModuleDescriptor::class.java)
        val resolutionFacade =
            KotlinResolutionFacadeForRepl(environment, container)
        val diagnostics = trace.bindingContext.diagnostics
        return ReplLineAnalysisResultWithStateless.Stateless(
            diagnostics,
            trace.bindingContext,
            resolutionFacade,
            moduleDescriptor,
            resultPropertyDescriptor,
        )
    }

}
