/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.NativeDefaultImportsProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.Path

fun createModuleDescriptor(
    environment: KotlinCoreEnvironment,
    tempDir: File,
    kotlinSources: List<String>,
): ModuleDescriptor {
    val testModuleRoot = tempDir.resolve("testModule")
    testModuleRoot.mkdirs()
    val testSourcesFiles = kotlinSources.mapIndexed { index, kotlinSource ->
        testModuleRoot.resolve("TestSources$index.kt").apply {
            writeText(kotlinSource)
        }
    }
    return createModuleDescriptor(environment, testSourcesFiles)
}

@OptIn(K1Deprecation::class)
fun createModuleDescriptor(
    environment: KotlinCoreEnvironment,
    kotlinFiles: List<File>,
    dependencyKlibs: List<Path> = emptyList(),
): ModuleDescriptor {

    val moduleDescriptorFactory = K1KlibMetadataModuleDescriptorFactoryImpl()

    val stdlibModuleDescriptor = moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
        library = loadKlib(kotlinNativeStdlibPath),
        languageVersionSettings = createLanguageVersionSettings(),
        storageManager = LockBasedStorageManager.NO_LOCKS,
    ).also { it.setDependencies(it) }

    val dependencyKlibDescriptors = dependencyKlibs.map { dependencyKlib ->
        moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = loadKlib(dependencyKlib),
            languageVersionSettings = createLanguageVersionSettings(),
            storageManager = LockBasedStorageManager.NO_LOCKS,
        )
    }

    dependencyKlibDescriptors.forEach { dependencyDescriptor ->
        dependencyDescriptor.setDependencies(
            dependencyKlibDescriptors + stdlibModuleDescriptor
        )
    }

    val moduleDescriptor = ModuleDescriptorImpl(
        moduleName = Name.special("<test_module>"),
        storageManager = LockBasedStorageManager.NO_LOCKS,
        builtIns = DefaultBuiltIns(loadBuiltInsFromCurrentClassLoader = false).apply {
            builtInsModule = stdlibModuleDescriptor.builtIns.builtInsModule
        },
        platform = NativePlatforms.unspecifiedNativePlatform,
        capabilities = mapOf(
            KlibModuleOrigin.CAPABILITY to CurrentKlibModuleOrigin,
        )
    )

    moduleDescriptor.setDependencies(
        ModuleDependenciesImpl(
            allDependencies = listOf(moduleDescriptor, stdlibModuleDescriptor) + dependencyKlibDescriptors,
            modulesWhoseInternalsAreVisible = emptySet(),
            directExpectedByDependencies = emptyList(),
            allExpectedByDependencies = emptySet()
        )
    )

    val projectContext = ProjectContext(environment.project, "test project context")

    val psiFactory = KtPsiFactory(environment.project)
    val kotlinPsiFiles = kotlinFiles.map { file -> psiFactory.createFile(file.name, KtTestUtil.doLoadFile(file)) }

    return FakeTopDownAnalyzerFacadeForNative.analyzeFilesWithGivenTrace(
        files = kotlinPsiFiles,
        trace = NoScopeRecordCliBindingTrace(environment.project),
        languageVersionSettings = createLanguageVersionSettings(),
        moduleContext = projectContext.withModule(moduleDescriptor)
    ).moduleDescriptor
}

fun createKotlinCoreEnvironment(
    disposable: Disposable, compilerConfiguration: CompilerConfiguration = createCompilerConfiguration(),
): KotlinCoreEnvironment {
    @OptIn(CoreEnvironmentDeprecation::class)
    return KotlinCoreEnvironment.createForTests(
        parentDisposable = disposable,
        initialConfiguration = compilerConfiguration,
        extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
    )
}

private fun createCompilerConfiguration(): CompilerConfiguration {
    val configuration = KotlinTestUtils.newConfiguration()
    configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, createLanguageVersionSettings())
    configuration.phaseConfig = createPhaseConfig(K2NativeCompilerArguments())
    return configuration
}

internal fun createLanguageVersionSettings() = LanguageVersionSettingsImpl(
    languageVersion = LanguageVersion.LATEST_STABLE,
    apiVersion = ApiVersion.LATEST_STABLE
)

private fun loadKlib(path: String): KotlinLibrary = KlibLoader { libraryPaths(path) }.loadOrFail()
private fun loadKlib(path: Path): KotlinLibrary = KlibLoader { libraryPaths(path) }.loadOrFail()

private fun KlibLoader.loadOrFail(): KotlinLibrary {
    val result = load()
    result.reportLoadingProblemsIfAny { _, message -> error(message) }
    return result.librariesStdlibFirst.single()
}

@K1Deprecation
private object FakeTopDownAnalyzerFacadeForNative {
    fun analyzeFilesWithGivenTrace(
        files: Collection<KtFile>,
        trace: BindingTrace,
        moduleContext: ModuleContext,
        languageVersionSettings: LanguageVersionSettings,
        compilerEnvironment: TargetEnvironment = CompilerEnvironment
    ): AnalysisResult {

        val analyzerForNative = createFakeTopDownAnalyzerForNative(
            moduleContext, trace,
            FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
            languageVersionSettings,
            compilerEnvironment
        )

        @Suppress("DEPRECATION_ERROR")
        analyzerForNative.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files as Collection<com.intellij.psi.PsiElement>)
        return AnalysisResult.success(trace.bindingContext, moduleContext.module)
    }

    private fun createFakeTopDownAnalyzerForNative(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        languageVersionSettings: LanguageVersionSettings,
        compilerEnvironment: TargetEnvironment = CompilerEnvironment
    ): LazyTopDownAnalyzer = createContainer("FakeTopDownAnalyzerForNative", FakeNativePlatformAnalyzerServices) {
        configureModule(
            moduleContext,
            NativePlatforms.unspecifiedNativePlatform,
            FakeNativePlatformAnalyzerServices,
            bindingTrace,
            languageVersionSettings,
            optimizingOptions = null,
            absentDescriptorHandlerClass = null
        )

        configureStandardResolveComponents()

        useInstance(declarationProviderFactory)
        useInstance(InlineConstTracker.DoNothing)
        compilerEnvironment.configure(this)
    }.apply {
        val moduleDescriptor = get<ModuleDescriptorImpl>()
        moduleDescriptor.initialize(get<KotlinCodeAnalyzer>().packageFragmentProvider)
    }.get<LazyTopDownAnalyzer>()
}

@K1Deprecation
private object FakeNativePlatformAnalyzerServices : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = NativePlatformConfigurator
    override val defaultImportsProvider: DefaultImportsProvider = NativeDefaultImportsProvider

    private object NativePlatformConfigurator : PlatformConfiguratorBase() {
        override fun configureModuleComponents(container: StorageComponentContainer) {}
    }
}
