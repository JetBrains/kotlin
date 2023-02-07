/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.analyze

import com.intellij.openapi.project.Project
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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.js.di.createContainerForJS
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.web.analyzer.WebAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
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
import org.jetbrains.kotlin.web.analyze.AbstractTopDownAnalyzerFacadeForWeb

abstract class AbstractTopDownAnalyzerFacadeForJS : AbstractTopDownAnalyzerFacadeForWeb {
    override fun analyzeFiles(
        files: Collection<KtFile>,
        project: Project,
        configuration: CompilerConfiguration,
        moduleDescriptors: List<ModuleDescriptor>,
        friendModuleDescriptors: List<ModuleDescriptor>,
        targetEnvironment: TargetEnvironment,
        thisIsBuiltInsModule: Boolean,
        customBuiltInsModule: ModuleDescriptor?
    ): WebAnalysisResult {
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
        @Suppress("UNCHECKED_CAST")
        context.module.setDependencies(dependencies.toList() as List<ModuleDescriptorImpl>, friendModuleDescriptors.toSet() as Set<ModuleDescriptorImpl>)

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

    override fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        configuration: CompilerConfiguration,
        targetEnvironment: TargetEnvironment,
        project: Project,
        additionalPackages: List<PackageFragmentProvider>
    ): WebAnalysisResult {
        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
        val expectActualTracker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing
        val inlineConstTracker = configuration.get(CommonConfigurationKeys.INLINE_CONST_TRACKER) ?: InlineConstTracker.DoNothing
        val enumWhenTracker = configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER) ?: EnumWhenTracker.DoNothing
        val languageVersionSettings = configuration.languageVersionSettings
        val packageFragment = configuration[WebConfigurationKeys.INCREMENTAL_DATA_PROVIDER]?.let {
            loadIncrementalCacheMetadata(it, moduleContext, lookupTracker, languageVersionSettings)
        }

        val container = createContainerForJS(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            languageVersionSettings,
            lookupTracker,
            expectActualTracker,
            inlineConstTracker,
            enumWhenTracker,
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
            is WebAnalysisResult -> result
            else -> {
                // AnalysisHandlerExtension returns a BindingContext, not BindingTrace. Therefore, synthesize one here.
                val bindingTrace = DelegatingBindingTrace(result.bindingContext, "DelegatingBindingTrace by AnalysisHandlerExtension")
                when (result) {
                    is AnalysisResult.RetryWithAdditionalRoots -> WebAnalysisResult.RetryWithAdditionalRoots(
                        bindingTrace,
                        result.moduleDescriptor,
                        result.additionalKotlinRoots
                    )
                    else -> WebAnalysisResult.success(bindingTrace, result.moduleDescriptor, result.shouldGenerateCode)
                }
            }
        }
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
    ): WebAnalysisResult {
        config.init()
        return analyzeFiles(
            files, config.project, config.configuration, config.moduleDescriptors, config.friendModuleDescriptors, config.targetEnvironment,
        )
    }
}
