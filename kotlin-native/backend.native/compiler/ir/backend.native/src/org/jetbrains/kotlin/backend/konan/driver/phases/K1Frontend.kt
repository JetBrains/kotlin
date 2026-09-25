/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazyImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.backend.konan.objcexport.StubRenderer
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.konan.config.emitLazyObjcHeaderFile
import org.jetbrains.kotlin.konan.config.objcGenerics
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.AnnotationResolverImpl
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.createContainer
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.BasicAbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.collections.plusAssign
import kotlin.io.path.Path
import kotlin.io.path.writeLines

data class K1FrontendPhaseOutput(val moduleDescriptor: ModuleDescriptor, val disposeCallback: () -> Unit)

@OptIn(K1Deprecation::class)
internal val K1FrontendPhase: NamedCompilerPhase<NativeBackendPhaseContext, KotlinCoreEnvironment, K1FrontendPhaseOutput?> = createSimpleNamedCompilerPhase(
        "Frontend",
        outputIfNotEnabled = { _, _, _, _ -> null }
) { context, input: KotlinCoreEnvironment ->
    val sourceFiles = input.getSourceFiles()

    check(context.config.produce != CompilerOutputKind.LIBRARY) {
        "Internal error: An attempt to run the 2nd compilation stage to produce ${CompilerOutputKind.LIBRARY}"
    }

    check(sourceFiles.isEmpty()) {
        "Internal error: no source files should have been passed here (${sourceFiles.first().virtualFilePath} in particular)\n" +
                "to produce binary (e.g. a ${context.config.produce.name.toLowerCaseAsciiOnly()})\n" +
                "KonanDriver.kt::splitOntoTwoStages() must transform such compilation into two-stage compilation. Please report this here: https://kotl.in/issue"
    }

    val frontendOutputs = setupModuleDescriptor(context)
    setupContainerAndDumpObjcHeader(context, frontendOutputs)
    val moduleDescriptor = frontendOutputs.moduleDescriptor
    context.config.configuration.sourcesModules = moduleDescriptor.getIncludedLibraryDescriptors(context.config).toSet() + moduleDescriptor

    K1FrontendPhaseOutput(moduleDescriptor) {
        val bindingContext = frontendOutputs.trace.bindingContext as CleanableBindingContext
        bindingContext.clear()
    }
}

@OptIn(K1Deprecation::class)
private data class FrontendOutputs(
        val moduleDescriptor: ModuleDescriptor,
        val trace: BindingTraceContext,
        val moduleContext: ModuleContext,
        val additionalPackages: List<PackageFragmentProvider>,
)

@OptIn(K1Deprecation::class)
private fun setupModuleDescriptor(context: NativeBackendPhaseContext): FrontendOutputs {
    val config = context.config
    val moduleName = Name.special("<${config.moduleId}>")

    val projectContext = ProjectContext(config.project, "TopDownAnalyzer for Konan")

    val builtIns = KonanBuiltIns(projectContext.storageManager)
    val module = ModuleDescriptorImpl(
            moduleName,
            projectContext.storageManager,
            builtIns,
            capabilities = mapOf(
                    KlibModuleOrigin.CAPABILITY to CurrentKlibModuleOrigin,
                    ImplicitIntegerCoercion.MODULE_CAPABILITY to CurrentKlibModuleOrigin.isCInteropLibrary()
            ),
            platform = NativePlatforms.unspecifiedNativePlatform
    )
    builtIns.builtInsModule = module
    val moduleContext = MutableModuleContextImpl(module, projectContext)

    val moduleDescriptorFactory = K1KlibMetadataModuleDescriptorFactoryImpl()
    val resolvedModuleDescriptors = KlibResolvedModuleDescriptorsFactoryImpl(moduleDescriptorFactory).createResolved2(
            // Note: The order of libraries is not important except for stdlib, which should go the first.
            libraries = config.loadedKlibs.all,
            storageManager = projectContext.storageManager,
            builtIns = module.builtIns,
            languageVersionSettings = config.languageVersionSettings,
            friendModuleFiles = config.loadedKlibs.friends.map { it.path }.toSet(),
            refinesModuleFiles = config.refinesModuleFiles,
            includedLibraryFiles = config.loadedKlibs.included.map { it.path }.toSet(),
            additionalDependencyModules = listOf(module),
            isForMetadataCompilation = config.metadataKlib
    )

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
    return FrontendOutputs(module, BindingTraceContext(projectContext.project), moduleContext, additionalPackages)
}

@OptIn(K1Deprecation::class)
private fun setupContainerAndDumpObjcHeader(context: NativeBackendPhaseContext, outputs: FrontendOutputs) {
    val moduleContext = outputs.moduleContext
    val container = createTopDownAnalyzerProviderForKonan(
            moduleContext, outputs.trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, emptyList()),
            context.config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!,
            outputs.additionalPackages
    ) {
        initObjCServicesInContainer(context.config)
    }
    container.postprocessComponents(context)
}

@OptIn(K1Deprecation::class)
private fun createTopDownAnalyzerProviderForKonan(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        languageVersionSettings: LanguageVersionSettings,
        additionalPackages: List<PackageFragmentProvider>,
        initContainer: StorageComponentContainer.() -> Unit
): ComponentProvider {
    return createContainer("TopDownAnalyzerForKonan", NativePlatformAnalyzerServices) {
        configureModule(moduleContext, NativePlatforms.unspecifiedNativePlatform, NativePlatformAnalyzerServices, bindingTrace,
                languageVersionSettings,
                optimizingOptions = null,
                absentDescriptorHandlerClass = BasicAbsentDescriptorHandler::class.java)

        useInstance(declarationProviderFactory)
        useImpl<AnnotationResolverImpl>()

        CompilerEnvironment.configure(this)

        useImpl<ResolveSession>()
        useImpl<LazyTopDownAnalyzer>()
        useInstance(InlineConstTracker.DoNothing)

        initContainer()
    }.apply {
        val packagePartProviders = mutableListOf(get<KotlinCodeAnalyzer>().packageFragmentProvider)
        val moduleDescriptor = get<ModuleDescriptorImpl>()
        packagePartProviders += additionalPackages
        moduleDescriptor.initialize(
                CompositePackageFragmentProvider(
                        packagePartProviders,
                        "CompositeProvider@createTopDownAnalyzerProviderForKonan for module ${moduleContext.module}"
                )
        )
    }
}

@OptIn(K1Deprecation::class)
private fun StorageComponentContainer.initObjCServicesInContainer(config: NativeSecondStageCompilationConfig) {
    if (config.configuration.emitLazyObjcHeaderFile.isNullOrEmpty()) return

    useImpl<ObjCExportLazyImpl>()
    useInstance(object : ObjCExportProblemCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(declaration: DeclarationDescriptor, text: String) {}
        override fun reportError(text: String) {}
        override fun reportError(declaration: DeclarationDescriptor, text: String) {}
        override fun reportException(throwable: Throwable) = throw throwable
    })

    useInstance(object : ObjCExportLazy.Configuration {
        override val frameworkName: String
            get() = config.fullExportedNamePrefix

        override fun isIncluded(moduleInfo: ModuleInfo): Boolean = true

        override fun getCompilerModuleName(moduleInfo: ModuleInfo): String {
            shouldNotBeCalled()
        }

        override val objcGenerics: Boolean
            get() = config.configuration.objcGenerics

        override val objcExportBlockExplicitParameterNames: Boolean
            get() = config.configuration.get(BinaryOptions.objcExportBlockExplicitParameterNames, false)

        override val disableSwiftMemberNameMangling: Boolean
            get() = config.configuration.getBoolean(BinaryOptions.objcExportDisableSwiftMemberNameMangling)

        override val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport
            get() = config.unitSuspendFunctionObjCExport

        override val ignoreInterfaceMethodCollisions: Boolean
            get() = config.configuration.getBoolean(BinaryOptions.objcExportIgnoreInterfaceMethodCollisions)
    })
}

@OptIn(K1Deprecation::class)
private fun ComponentProvider.postprocessComponents(context: NativeBackendPhaseContext) {
    context.config.configuration.emitLazyObjcHeaderFile?.takeIf { it.isNotEmpty() }?.let {
        this.get<ObjCExportLazy>().dumpObjCHeader(it, context.shouldExportKDoc())
    }
}

private fun ObjCExportLazy.dumpObjCHeader(outputFile: String, shouldExportKDoc: Boolean) {
    val lines = this.generateBase().flatMap {
        StubRenderer.render(it, shouldExportKDoc) + listOf("")
    }

    Path(outputFile).writeLines(lines)
}
