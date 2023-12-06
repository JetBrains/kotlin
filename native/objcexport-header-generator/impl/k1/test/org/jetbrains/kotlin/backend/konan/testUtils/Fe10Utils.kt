/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createFlexiblePhaseConfig
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

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

fun createModuleDescriptor(
    environment: KotlinCoreEnvironment,
    kotlinFiles: List<File>,
): ModuleDescriptor {
    val psiFactory = KtPsiFactory(environment.project)
    val kotlinPsiFiles = kotlinFiles.map { file -> psiFactory.createFile(file.name, KtTestUtil.doLoadFile(file)) }

    val analysisResult = CommonResolverForModuleFactory.analyzeFiles(
        files = kotlinPsiFiles,
        moduleName = Name.special("<test_module>"),
        dependOnBuiltIns = true,
        languageVersionSettings = environment.configuration.languageVersionSettings,
        targetPlatform = CommonPlatforms.defaultCommonPlatform,
        targetEnvironment = CompilerEnvironment,
        dependenciesContainer = DependenciesContainerImpl,
    ) { content ->
        environment.createPackagePartProvider(content.moduleContentScope)
    }

    return analysisResult.moduleDescriptor
}

fun createKotlinCoreEnvironment(
    disposable: Disposable, compilerConfiguration: CompilerConfiguration = createCompilerConfiguration(),
): KotlinCoreEnvironment {
    return KotlinCoreEnvironment.createForTests(
        parentDisposable = disposable,
        initialConfiguration = compilerConfiguration,
        extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
    )
}

private fun createCompilerConfiguration(): CompilerConfiguration {
    val configuration = KotlinTestUtils.newConfiguration()
    configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, createLanguageVersionSettings())
    configuration.put(CLIConfigurationKeys.FLEXIBLE_PHASE_CONFIG, createFlexiblePhaseConfig(K2NativeCompilerArguments()))
    return configuration
}

private fun createLanguageVersionSettings() = LanguageVersionSettingsImpl(
    languageVersion = LanguageVersion.LATEST_STABLE,
    apiVersion = ApiVersion.LATEST_STABLE
)

private object DependenciesContainerImpl : CommonDependenciesContainer {
    override val moduleInfos: List<ModuleInfo> get() = listOf(DefaultBuiltInsModuleInfo, KotlinNativeStdlibModuleInfo)
    override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
    override val refinesModuleInfos: List<ModuleInfo> get() = emptyList()
    override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
    override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null

    private val klibFactory = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

    private val stdlibModuleDescriptor = klibFactory.DefaultDeserializedDescriptorFactory.createDescriptor(
        library = resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(kotlinNativeStdlibPath)),
        languageVersionSettings = createLanguageVersionSettings(),
        builtIns = DefaultBuiltIns.Instance,
        storageManager = LockBasedStorageManager.NO_LOCKS,
        packageAccessHandler = null
    ).also { it.setDependencies(it) }

    override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor {
        if (moduleInfo == DefaultBuiltInsModuleInfo) return DefaultBuiltIns.Instance.builtInsModule
        if (moduleInfo == KotlinNativeStdlibModuleInfo) return stdlibModuleDescriptor
        error("Unknown module info $moduleInfo")
    }
}

private object DefaultBuiltInsModuleInfo : ModuleInfo {
    override val name get() = DefaultBuiltIns.Instance.builtInsModule.name
    override fun dependencies() = listOf(this)
    override fun dependencyOnBuiltIns() = ModuleInfo.DependencyOnBuiltIns.LAST
    override val platform get() = NativePlatforms.unspecifiedNativePlatform
    override val analyzerServices get() = NativePlatformAnalyzerServices
}

private object KotlinNativeStdlibModuleInfo : ModuleInfo {
    override val analyzerServices: PlatformDependentAnalyzerServices = NativePlatformAnalyzerServices
    override val name: Name = Name.special("<stdlib>")
    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform
    override fun dependencies(): List<ModuleInfo> = listOf(this)
}