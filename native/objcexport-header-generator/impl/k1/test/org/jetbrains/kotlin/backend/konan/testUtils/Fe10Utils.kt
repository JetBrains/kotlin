/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createFlexiblePhaseConfig
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.context.withModule
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.FakeTopDownAnalyzerFacadeForNative
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
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

fun createModuleDescriptor(
    environment: KotlinCoreEnvironment,
    kotlinFiles: List<File>,
    dependencyKlibs: List<Path> = emptyList(),
): ModuleDescriptor {

    val klibFactory = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

    val stdlibModuleDescriptor = klibFactory.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
        library = resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(kotlinNativeStdlibPath)),
        languageVersionSettings = createLanguageVersionSettings(),
        storageManager = LockBasedStorageManager.NO_LOCKS,
        packageAccessHandler = null
    ).also { it.setDependencies(it) }

    val dependencyKlibDescriptors = dependencyKlibs.map { dependencyKlib ->
        klibFactory.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = resolveSingleFileKlib(org.jetbrains.kotlin.konan.file.File(dependencyKlib)),
            languageVersionSettings = createLanguageVersionSettings(),
            storageManager = LockBasedStorageManager.NO_LOCKS,
            packageAccessHandler = null,
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
        builtIns = KonanBuiltIns(LockBasedStorageManager.NO_LOCKS).apply {
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

internal fun createLanguageVersionSettings() = LanguageVersionSettingsImpl(
    languageVersion = LanguageVersion.LATEST_STABLE,
    apiVersion = ApiVersion.LATEST_STABLE
)
