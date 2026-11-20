/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.K1KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.getIncludedLibraryDescriptors
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazy
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportLazyImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.backend.konan.objcexport.StubRenderer
import org.jetbrains.kotlin.backend.konan.sourcesModules
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.konan.config.emitLazyObjcHeaderFile
import org.jetbrains.kotlin.konan.config.objcGenerics
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import kotlin.io.path.Path
import kotlin.io.path.writeLines

data class K1FrontendPhaseOutput(val moduleDescriptor: ModuleDescriptor)

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

    val moduleDescriptor = setupModuleDescriptor(context)
    dumpObjcHeader(context, moduleDescriptor)
    context.config.configuration.sourcesModules = moduleDescriptor.getIncludedLibraryDescriptors(context.config).toSet() + moduleDescriptor

    K1FrontendPhaseOutput(moduleDescriptor)
}

@OptIn(K1Deprecation::class)
private fun setupModuleDescriptor(context: NativeBackendPhaseContext): ModuleDescriptor {
    val config = context.config
    val moduleName = Name.special("<${config.moduleId}>")

    val storageManager = LockBasedStorageManager.createWithExceptionHandling(
            "Manger for ObjC export at native second stage",
            LockBasedStorageManager.ExceptionHandlingStrategy.THROW,
            { ProgressManager.checkCanceled() },
            { throw ProcessCanceledException(it) }
    )

    val builtIns = DefaultBuiltIns(loadBuiltInsFromCurrentClassLoader = false)
    val module = ModuleDescriptorImpl(
            moduleName,
            storageManager,
            builtIns,
            capabilities = mapOf(KlibModuleOrigin.CAPABILITY to CurrentKlibModuleOrigin),
            platform = NativePlatforms.unspecifiedNativePlatform
    )
    builtIns.builtInsModule = module

    val moduleDescriptorFactory = K1KlibMetadataModuleDescriptorFactoryImpl()
    val resolvedModuleDescriptors = KlibResolvedModuleDescriptorsFactoryImpl(moduleDescriptorFactory).createResolved2(
            // Note: The order of libraries is not important except for stdlib, which should go the first.
            libraries = config.loadedKlibs.all,
            storageManager = storageManager,
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
        // [K][Suspend]FunctionN belong to stdlib.
        additionalPackages += functionInterfacePackageFragmentProvider(storageManager, module)
    }
    module.initialize(
            CompositePackageFragmentProvider(
                    additionalPackages,
                    "CompositeProvider@createTopDownAnalyzerProviderForKonan for module $module"
            )
    )

    return module
}

private fun dumpObjcHeader(context: NativeBackendPhaseContext, moduleDescriptor: ModuleDescriptor) {
    val file = context.config.configuration.emitLazyObjcHeaderFile ?: return
    val translator = initObjCExportTranslator(context.config, moduleDescriptor.builtIns) ?: return
    translator.dumpObjCHeader(file, context.shouldExportKDoc())
}

private fun initObjCExportTranslator(config: NativeSecondStageCompilationConfig, builtIns: KotlinBuiltIns): ObjCExportLazy? {
    if (config.configuration.emitLazyObjcHeaderFile.isNullOrEmpty()) return null

    val problemsCollector = object : ObjCExportProblemCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(declaration: DeclarationDescriptor, text: String) {}
        override fun reportError(text: String) {}
        override fun reportError(declaration: DeclarationDescriptor, text: String) {}
        override fun reportException(throwable: Throwable) = throw throwable
    }
    val configuration = object : ObjCExportLazy.Configuration {
        override val frameworkName: String
            get() = config.fullExportedNamePrefix

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
    }

    val objCExportLazy = ObjCExportLazyImpl(
            languageVersionSettings = config.languageVersionSettings,
            configuration = configuration,
            problemCollector = problemsCollector,
            builtIns = builtIns,
    )
    return objCExportLazy
}

private fun ObjCExportLazy.dumpObjCHeader(outputFile: String, shouldExportKDoc: Boolean) {
    val lines = this.generateBase().flatMap {
        StubRenderer.render(it, shouldExportKDoc) + listOf("")
    }

    Path(outputFile).writeLines(lines)
}
