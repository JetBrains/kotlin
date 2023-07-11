/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.driver.phases.FrontendContext
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

internal object TopDownAnalyzerFacadeForKonan {

    private val nativeFactories = KlibMetadataFactories(::KonanBuiltIns, NullFlexibleTypeDeserializer)

    fun analyzeFiles(files: Collection<KtFile>, context: FrontendContext): AnalysisResult {
        val config = context.config
        val moduleName = Name.special("<${config.moduleId}>")

        val projectContext = ProjectContext(config.project, "TopDownAnalyzer for Konan")

        val module = nativeFactories.DefaultDescriptorFactory.createDescriptorAndNewBuiltIns(
                moduleName, projectContext.storageManager, origin = CurrentKlibModuleOrigin)
        val moduleContext = MutableModuleContextImpl(module, projectContext)

        val resolvedModuleDescriptors = nativeFactories.DefaultResolvedDescriptorsFactory.createResolved(
                config.resolvedLibraries, projectContext.storageManager, module.builtIns, config.languageVersionSettings,
                config.friendModuleFiles, config.refinesModuleFiles,
                config.resolve.includedLibraries.map { it.libraryFile }.toSet(), listOf(module),
                isForMetadataCompilation = config.metadataKlib)

        val additionalPackages = mutableListOf<PackageFragmentProvider>()
        if (!module.isNativeStdlib()) {
            module.setDependencies(ModuleDependenciesImpl(
                    allDependencies =
                    listOf(module) + resolvedModuleDescriptors.resolvedDescriptors + resolvedModuleDescriptors.forwardDeclarationsModule,
                    modulesWhoseInternalsAreVisible = resolvedModuleDescriptors.friendModules,
                    directExpectedByDependencies = resolvedModuleDescriptors.refinesModules.toList(),
                    allExpectedByDependencies = resolvedModuleDescriptors.refinesModules
            ))
        } else {
            assert(resolvedModuleDescriptors.resolvedDescriptors.isEmpty())
            moduleContext.setDependencies(module)
            // [K][Suspend]FunctionN belong to stdlib.
            additionalPackages += functionInterfacePackageFragmentProvider(projectContext.storageManager, module)
        }

        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), moduleContext, context, projectContext, additionalPackages)
    }

    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            context: FrontendContext,
            projectContext: ProjectContext,
            additionalPackages: List<PackageFragmentProvider> = emptyList()
    ): AnalysisResult {

        // we print out each file we compile if frontend phase is verbose
        files.takeIf {
            context.shouldPrintFiles()
        }?.forEach(::println)

        val container = createTopDownAnalyzerProviderForKonan(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                context.config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!,
                additionalPackages
        ) {
            initContainer(context.config)
        }.apply {
            postprocessComponents(context, files)
        }

        val analyzerForKonan = container.get<LazyTopDownAnalyzer>()
        val project = context.config.project
        val moduleDescriptor = moduleContext.module
        val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)

        // Mimic the behavior in the jvm frontend. The extensions have 2 chances to override the normal analysis:
        // * If any of the extensions returns a non-null result, use it. Otherwise do the normal analysis.
        // * `analysisCompleted` can be used to override the result, too.
        var result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
            extension.doAnalysis(project, moduleDescriptor, projectContext, files, trace, container)
        } ?: run {
            analyzerForKonan.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
            AnalysisResult.success(trace.bindingContext, moduleDescriptor)
        }

        result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
            extension.analysisCompleted(project, moduleDescriptor, trace, files)
        } ?: result

        return result
    }

    fun checkForErrors(files: Collection<KtFile>, bindingContext: BindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        for (file in files) {
            AnalyzingUtils.checkForSyntacticErrors(file)
        }
    }
}
