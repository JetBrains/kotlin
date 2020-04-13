/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.declarations.*
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KotlinResolutionFacade
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzerBase
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities

class IdeLikeReplCodeAnalyzer(private val environment: KotlinCoreEnvironment) : ReplCodeAnalyzerBase(environment, CliBindingTrace()) {
    interface ReplLineAnalysisResultWithStateless : ReplLineAnalysisResult {
        // Result of stateless analyse, which may be used for reporting errors
        // without code generation
        data class Stateless(
            override val diagnostics: Diagnostics,
            val bindingContext: BindingContext,
            val resolutionFacade: KotlinResolutionFacade,
            val moduleDescriptor: ModuleDescriptor
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

        topDownAnalyzer.analyzeDeclarations(topDownAnalysisContext.topDownAnalysisMode, listOf(linePsi) + importedScripts)

        val moduleDescriptor = container.getService(ModuleDescriptor::class.java)
        val resolutionFacade = KotlinResolutionFacade(environment, container)
        val diagnostics = trace.bindingContext.diagnostics
        return ReplLineAnalysisResultWithStateless.Stateless(
            diagnostics,
            trace.bindingContext,
            resolutionFacade,
            moduleDescriptor
        )
    }

}
