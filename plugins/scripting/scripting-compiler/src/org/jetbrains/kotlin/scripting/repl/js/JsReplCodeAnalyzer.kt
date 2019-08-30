/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.frontend.js.di.createTopDownAnalyzerForJs
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.scripting.compiler.plugin.repl.ReplCodeAnalyzer
import org.jetbrains.kotlin.scripting.definitions.ScriptPriorities

class JsReplCodeAnalyzer(private val project: Project, private val dependencies: List<ModuleDescriptor>) {

    private val replState = ReplCodeAnalyzer.ResettableAnalyzerState()
    val trace: BindingTraceContext = NoScopeRecordCliBindingTrace()

    private val builtIns: KotlinBuiltIns = dependencies.single { it.allDependencyModules.isEmpty() }.builtIns
    lateinit var context: MutableModuleContext
    private fun createTopDownAnalyzerJS(files: Collection<KtFile>): LazyTopDownAnalyzer {
        context = ContextForNewModule(
            ProjectContext(project, "TopDownAnalyzer for JS"),
            Name.special("<script>"),
            builtIns,
            platform = null
        )

        val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
        val lookupTracker = LookupTracker.DO_NOTHING
        val expectActualTracker = ExpectActualTracker.DoNothing
        val additionalPackages = mutableListOf<PackageFragmentProvider>()

        context.module.setDependencies(dependencies.map { it as ModuleDescriptorImpl } + context.module)

        return createTopDownAnalyzerForJs(
            context, trace,
            FileBasedDeclarationProviderFactory(context.storageManager, files),
            languageVersionSettings,
            lookupTracker,
            expectActualTracker,
            additionalPackages
        )
    }

    fun analyzeReplLine(linePsi: KtFile, codeLine: ReplCodeLine): Diagnostics {
        trace.clearDiagnostics()

        linePsi.script!!.putUserData(ScriptPriorities.PRIORITY_KEY, codeLine.no)

        replState.submitLine(linePsi, codeLine)

        val analyzer = createTopDownAnalyzerJS(listOf(linePsi))
        val context = analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(linePsi))

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }

        if (hasErrors) {
            replState.lineFailure(linePsi, codeLine)
        } else {
            val scriptDescriptor = context.scripts[linePsi.script]!!
            replState.lineSuccess(linePsi, codeLine, scriptDescriptor)
        }
        return diagnostics
    }
}
