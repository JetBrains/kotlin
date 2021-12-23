/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.js

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.frontend.js.di.createTopDownAnalyzerForJs
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

abstract class AbstractJsScriptlikeCodeAnalyser(
    private val environment: KotlinCoreEnvironment,
    private val dependencies: List<ModuleDescriptor>
) {

    protected class JsScriptAnalysisResult(
        val moduleDescriptor: ModuleDescriptor,
        private val scriptDescriptor: ClassDescriptorWithResolutionScopes?,
        val bindingContext: BindingContext
    ) {
        val isSuccess: Boolean get() = scriptDescriptor != null
        val script: ClassDescriptorWithResolutionScopes get() = scriptDescriptor ?: error("Error occurred")
    }

    protected fun analysisImpl(psi: KtFile): JsScriptAnalysisResult {
        val trace: BindingTraceContext = NoScopeRecordCliBindingTrace()
        val project = environment.project
        val builtIns: KotlinBuiltIns = dependencies.single { it.allDependencyModules.isEmpty() }.builtIns
        val moduleContext = ContextForNewModule(
            ProjectContext(project, "TopDownAnalyzer for JS Script"),
            Name.special("<script>"),
            builtIns,
            platform = null
        )
        val languageVersionSettings = environment.configuration.languageVersionSettings
        val lookupTracker = LookupTracker.DO_NOTHING
        val expectActualTracker = ExpectActualTracker.DoNothing
        val inlineConstTracker = InlineConstTracker.DoNothing
        val additionalPackages = emptyList<PackageFragmentProvider>()
        val moduleDescriptor = moduleContext.module

        moduleDescriptor.setDependencies(dependencies.map { it as ModuleDescriptorImpl } + moduleDescriptor)

        val analyzer = createTopDownAnalyzerForJs(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, listOf(psi)),
            languageVersionSettings,
            lookupTracker,
            expectActualTracker,
            inlineConstTracker,
            additionalPackages,
            CompilerEnvironment,
        )
        val analyzerContext = analyzer.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, listOf(psi))

        val diagnostics = trace.bindingContext.diagnostics
        val hasErrors = diagnostics.any { it.severity == Severity.ERROR }
        val scriptDescriptor = analyzerContext.scripts[psi.script]

        assert(scriptDescriptor != null || hasErrors) { "If no errors occurred script descriptor has to be existed" }

        return JsScriptAnalysisResult(moduleDescriptor, scriptDescriptor, trace.bindingContext)
    }
}