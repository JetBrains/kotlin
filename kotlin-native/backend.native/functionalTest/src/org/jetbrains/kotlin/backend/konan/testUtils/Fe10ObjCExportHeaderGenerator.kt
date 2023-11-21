/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.konan.KlibFactories
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.Companion.KONAN_HOME
import org.jetbrains.kotlin.backend.konan.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportHeaderGeneratorImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createFlexiblePhaseConfig
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
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
import org.jetbrains.kotlin.test.testFramework.MockProjectEx
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

object Fe10ObjCExportHeaderGenerator : AbstractObjCExportHeaderGeneratorTest.ObjCExportHeaderGenerator {
    override fun generateHeaders(disposable: Disposable, root: File): String {
        val headerGenerator = createObjCExportHeaderGenerator(disposable, root)
        headerGenerator.translateModuleDeclarations()
        return headerGenerator.build().joinToString(System.lineSeparator())
    }

    private fun createObjCExportHeaderGenerator(disposable: Disposable, root: File): ObjCExportHeaderGenerator {
        val compilerConfiguration = createCompilerConfiguration()

        val mapper = ObjCExportMapper(
            unitSuspendFunctionExport = UnitSuspendFunctionObjCExport.DEFAULT
        )

        val namer = ObjCExportNamerImpl(
            mapper = mapper,
            builtIns = DefaultBuiltIns.Instance,
            local = false,
            problemCollector = ObjCExportProblemCollector.SILENT,
            configuration = object : ObjCExportNamer.Configuration {
                override val topLevelNamePrefix: String get() = ""
                override fun getAdditionalPrefix(module: ModuleDescriptor): String? = null
                override val objcGenerics: Boolean = true
            }

        )

        val environment: KotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
            parentDisposable = disposable,
            initialConfiguration = compilerConfiguration,
            extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
        )

        val phaseContext = BasicPhaseContext(
            KonanConfig(environment.project, compilerConfiguration)
        )

        val kotlinFiles = root.walkTopDown().filter { it.isFile }.filter { it.extension == "kt" }.toList()

        return ObjCExportHeaderGeneratorImpl(
            context = phaseContext,
            moduleDescriptors = listOf(getModuleDescriptor(environment, kotlinFiles)),
            mapper = mapper,
            namer = namer,
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true
        )
    }

    private fun getModuleDescriptor(
        environment: KotlinCoreEnvironment, kotlinFiles: List<File>
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


    private object DependenciesContainerImpl : CommonDependenciesContainer {
        override val moduleInfos: List<ModuleInfo> get() = listOf(DefaultBuiltInsModuleInfo, KotlinNativeStdlibModuleInfo)
        override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
        override val refinesModuleInfos: List<ModuleInfo> get() = emptyList()
        override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
        override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null

        private val stdlibModuleDescriptor = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptor(
            library = resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File("$konanHomePath/klib/common/stdlib")),
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

    private fun createCompilerConfiguration(): CompilerConfiguration {
        val configuration = KotlinTestUtils.newConfiguration()
        configuration.put(KONAN_HOME, konanHomePath)
        configuration.put(CLIConfigurationKeys.FLEXIBLE_PHASE_CONFIG, createFlexiblePhaseConfig(K2NativeCompilerArguments()))
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, createLanguageVersionSettings())
        configuration.put(KonanConfigKeys.PRODUCE, CompilerOutputKind.FRAMEWORK)
        configuration.put(KonanConfigKeys.AUTO_CACHEABLE_FROM, emptyList())
        configuration.put(KonanConfigKeys.CACHE_DIRECTORIES, emptyList())
        configuration.put(KonanConfigKeys.CACHED_LIBRARIES, emptyMap())
        configuration.put(KonanConfigKeys.FRAMEWORK_IMPORT_HEADERS, emptyList())
        configuration.put(KonanConfigKeys.EXPORT_KDOC, true)
        return configuration
    }

    private fun createLanguageVersionSettings() = LanguageVersionSettingsImpl(
        languageVersion = LanguageVersion.LATEST_STABLE,
        apiVersion = ApiVersion.LATEST_STABLE
    )
}