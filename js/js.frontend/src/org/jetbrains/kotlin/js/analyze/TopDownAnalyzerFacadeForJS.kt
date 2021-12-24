/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.analyze

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.js.di.createContainerForJS
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.js.resolve.MODULE_KIND
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.serialization.js.PackagesWithHeaderMetadata
import org.jetbrains.kotlin.utils.JsMetadataVersion

abstract class AbstractTopDownAnalyzerFacadeForJS {

    fun analyzeFiles(
        files: Collection<KtFile>,
        project: Project,
        configuration: CompilerConfiguration,
        moduleDescriptors: List<ModuleDescriptorImpl>,
        friendModuleDescriptors: List<ModuleDescriptorImpl>,
        targetEnvironment: TargetEnvironment,
        thisIsBuiltInsModule: Boolean = false,
        customBuiltInsModule: ModuleDescriptorImpl? = null
    ): JsAnalysisResult {
        require(!thisIsBuiltInsModule || customBuiltInsModule == null) {
            "Can't simultaneously use custom built-ins module and set current module as built-ins"
        }

        val builtIns = when {
            thisIsBuiltInsModule -> DefaultBuiltIns(loadBuiltInsFromCurrentClassLoader = false)
            customBuiltInsModule != null -> customBuiltInsModule.builtIns
            else -> JsPlatformAnalyzerServices.builtIns
        }

        val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!
        val context = ContextForNewModule(
            ProjectContext(project, "TopDownAnalyzer for JS"),
            Name.special("<$moduleName>"),
            builtIns,
            platform = JsPlatforms.defaultJsPlatform
        )

        val additionalPackages = mutableListOf<PackageFragmentProvider>()

        if (thisIsBuiltInsModule) {
            builtIns.builtInsModule = context.module
            additionalPackages += functionInterfacePackageFragmentProvider(context.storageManager, context.module)
        }

        val dependencies = mutableSetOf(context.module) + moduleDescriptors + builtIns.builtInsModule
        context.module.setDependencies(dependencies.toList(), friendModuleDescriptors.toSet())

        val moduleKind = configuration.get(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)

        val trace = BindingTraceContext()
        trace.record(MODULE_KIND, context.module, moduleKind)
        return analyzeFilesWithGivenTrace(files, trace, context, configuration, targetEnvironment, project, additionalPackages)
    }

    protected abstract fun loadIncrementalCacheMetadata(
        incrementalData: IncrementalDataProvider,
        moduleContext: ModuleContext,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
    ): PackageFragmentProvider

    fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        configuration: CompilerConfiguration,
        targetEnvironment: TargetEnvironment,
        project: Project,
        additionalPackages: List<PackageFragmentProvider> = emptyList()
    ): JsAnalysisResult {
        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
        val expectActualTracker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing
        val inlineConstTracker = configuration.get(CommonConfigurationKeys.INLINE_CONST_TRACKER) ?: InlineConstTracker.DoNothing
        val languageVersionSettings = configuration.languageVersionSettings
        val packageFragment = configuration[JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER]?.let {
            loadIncrementalCacheMetadata(it, moduleContext, lookupTracker, languageVersionSettings)
        }

        val container = createContainerForJS(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            languageVersionSettings,
            lookupTracker,
            expectActualTracker,
            inlineConstTracker,
            additionalPackages + listOfNotNull(packageFragment),
            targetEnvironment,
        )

        val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)

        // Mimic the behavior in the jvm frontend. The extensions have 2 chances to override the normal analysis:
        // * If any of the extensions returns a non-null result, it. Otherwise do the normal analysis.
        // * `analysisCompleted` can be used to override the result, too.
        var result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
            extension.doAnalysis(project, moduleContext.module, moduleContext, files, trace, container)
        } ?: run {
            container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
            AnalysisResult.success(trace.bindingContext, moduleContext.module)
        }

        result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
            extension.analysisCompleted(project, moduleContext.module, trace, files)
        } ?: result

        return when (result) {
            is JsAnalysisResult -> result
            else -> {
                // AnalysisHandlerExtension returns a BindingContext, not BindingTrace. Therefore, synthesize one here.
                val bindingTrace = DelegatingBindingTrace(result.bindingContext, "DelegatingBindingTrace by AnalysisHandlerExtension")
                when (result) {
                    is AnalysisResult.RetryWithAdditionalRoots -> JsAnalysisResult.RetryWithAdditionalRoots(
                        bindingTrace,
                        result.moduleDescriptor,
                        result.additionalKotlinRoots
                    )
                    else -> JsAnalysisResult.success(bindingTrace, result.moduleDescriptor, result.shouldGenerateCode)
                }
            }
        }
    }

    fun checkForErrors(allFiles: Collection<KtFile>, bindingContext: BindingContext, errorPolicy: ErrorTolerancePolicy): Boolean {
        var hasErrors = false
        try {
            AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        } catch (ex: Exception) {
            if (!errorPolicy.allowSemanticErrors) {
                throw ex
            } else {
                hasErrors = true
            }
        }

        try {
            for (file in allFiles) {
                AnalyzingUtils.checkForSyntacticErrors(file)
            }
        } catch (ex: Exception) {
            if (!errorPolicy.allowSyntaxErrors) {
                throw ex
            } else {
                hasErrors = true
            }
        }

        return hasErrors
    }

    companion object {
        fun newModuleSearchScope(project: Project, files: Collection<KtFile>): GlobalSearchScope {
            // In case of separate modules, the source module scope generally consists of the following scopes:
            // 1) scope which only contains passed Kotlin source files (.kt and .kts)
            // 2) scope which contains all Java source files (.java) in the project
            return GlobalSearchScope.filesScope(project, files.map { it.virtualFile }.toSet()).uniteWith(AllJsSourcesInProjectScope(project))
        }
    }

    // TODO: limit this scope to the Java source roots, which the module has in its CONTENT_ROOTS
    class AllJsSourcesInProjectScope(project: Project) : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
        // 'isDirectory' check is needed because otherwise directories such as 'frontend.java' would be recognized
        // as Java source files, which makes no sense
        override fun contains(file: VirtualFile) =
            file.name.contains(Regex(".js")) && !file.isDirectory

        override fun toString() = "All JavaScript sources in the project"
    }
}

object TopDownAnalyzerFacadeForJS : AbstractTopDownAnalyzerFacadeForJS() {

    override fun loadIncrementalCacheMetadata(
        incrementalData: IncrementalDataProvider,
        moduleContext: ModuleContext,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
    ): PackageFragmentProvider {
        val metadata = PackagesWithHeaderMetadata(
            incrementalData.headerMetadata,
            incrementalData.compiledPackageParts.values.map { it.metadata },
            JsMetadataVersion(*incrementalData.metadataVersion)
        )
        return KotlinJavascriptSerializationUtil.readDescriptors(
            metadata, moduleContext.storageManager, moduleContext.module,
            CompilerDeserializationConfiguration(languageVersionSettings), lookupTracker
        )
    }

    @JvmStatic
    fun analyzeFiles(
        files: Collection<KtFile>,
        config: JsConfig
    ): JsAnalysisResult {
        config.init()
        return analyzeFiles(
            files, config.project, config.configuration, config.moduleDescriptors, config.friendModuleDescriptors, config.targetEnvironment,
        )
    }
}
