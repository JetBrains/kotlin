/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPerformanceManagerFor
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.nativeTargets
import org.jetbrains.kotlin.native.pipeline.NativeKlibCliPipeline
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import java.nio.file.Path

fun createModuleDescriptor(tempDir: File, kotlinSources: List<String>): ModuleDescriptor {
    val testModuleRoot = tempDir.resolve("testModule")
    testModuleRoot.mkdirs()
    val testSourcesFiles = kotlinSources.mapIndexed { index, kotlinSource ->
        testModuleRoot.resolve("TestSources$index.kt").apply {
            writeText(kotlinSource)
        }
    }
    return createModuleDescriptor(testSourcesFiles)
}

@Suppress("UNUSED_PARAMETER")
@OptIn(K1Deprecation::class)
fun createModuleDescriptor(kotlinFiles: List<File>, dependencyKlibs: List<Path> = emptyList()): ModuleDescriptor {
    val moduleDescriptorFactory = K1KlibMetadataModuleDescriptorFactoryImpl()

    val stdlibModuleDescriptor = moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
        library = loadKlib(kotlinNativeStdlibPath),
        languageVersionSettings = createLanguageVersionSettings(),
        storageManager = LockBasedStorageManager.NO_LOCKS,
    ).also { it.setDependencies(it) }

    val dependencyLibraries = dependencyKlibs.map { loadKlib(it) }

    val dependencyKlibDescriptors = dependencyLibraries.map { dependencyLibrary ->
        moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = dependencyLibrary,
            languageVersionSettings = createLanguageVersionSettings(),
            storageManager = LockBasedStorageManager.NO_LOCKS,
        )
    }

    dependencyKlibDescriptors.forEach { dependencyDescriptor ->
        dependencyDescriptor.setDependencies(
            dependencyKlibDescriptors + stdlibModuleDescriptor
        )
    }

    val testModuleKlib = compileTestModuleKlib(kotlinFiles, dependencyKlibs, dependencyLibraries)

    val moduleDescriptor = moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
        library = loadKlib(testModuleKlib),
        languageVersionSettings = createLanguageVersionSettings(),
        storageManager = LockBasedStorageManager.NO_LOCKS,
    )

    moduleDescriptor.setDependencies(
        ModuleDependenciesImpl(
            allDependencies = listOf(moduleDescriptor, stdlibModuleDescriptor) + dependencyKlibDescriptors,
            modulesWhoseInternalsAreVisible = emptySet(),
            directExpectedByDependencies = emptyList(),
            allExpectedByDependencies = emptySet()
        )
    )

    return moduleDescriptor
}

private fun compileTestModuleKlib(
    kotlinFiles: List<File>,
    dependencyKlibs: List<Path>,
    dependencyLibraries: List<KotlinLibrary>,
): File {
    val targets = dependencyLibraries.flatMapTo(LinkedHashSet()) { it.nativeTargets }
    val target = when {
        targets.isEmpty() -> null
        targets.size == 1 -> targets.single()
        else -> error("Dependency klibs of the test module have mixed targets: $targets")
    }

    val outputKlib = FileUtil.createTempDirectory("objcExportTestModule", null).resolve("testModule.klib")

    val arguments = K2NativeCompilerArguments().apply {
        disableDefaultScriptingPlugin = true
        produce = "library"
        target?.let { this.target = it }
        outputName = outputKlib.path
        moduleName = "test_module"
        exportKDoc = true
        companionBlocksAndExtensions = true
        optIn = arrayOf(
            "kotlin.experimental.ExperimentalObjCEnum",
            "kotlin.experimental.ExperimentalObjCName",
            "kotlin.native.internal.InternalForKotlinNative",
            "kotlinx.cinterop.ExperimentalForeignApi",
        )
        kotlinHome = konanHomePath
        libraries = dependencyKlibs.map { it.toString() }.toTypedArray()
        freeArgs = kotlinFiles.map { it.absolutePath }
    }

    val messageCollector = MessageCollectorImpl()
    val exitCode = NativeKlibCliPipeline(
        defaultPerformanceManager = createPerformanceManagerFor(NativePlatforms.unspecifiedNativePlatform),
        isNativeOneStage = false,
    ).execute(arguments, Services.EMPTY, messageCollector)

    check(exitCode == ExitCode.OK && !messageCollector.hasErrors()) {
        "Failed to compile the ObjC Export test module to a klib:\n$messageCollector"
    }

    return outputKlib
}

internal fun createLanguageVersionSettings() = LanguageVersionSettingsImpl(
    languageVersion = LanguageVersion.LATEST_STABLE,
    apiVersion = ApiVersion.LATEST_STABLE
)

private fun loadKlib(path: String): KotlinLibrary = KlibLoader { libraryPaths(path) }.loadOrFail()
private fun loadKlib(path: Path): KotlinLibrary = KlibLoader { libraryPaths(path) }.loadOrFail()
private fun loadKlib(file: File): KotlinLibrary = KlibLoader { libraryPaths(file.toPath()) }.loadOrFail()

private fun KlibLoader.loadOrFail(): KotlinLibrary {
    val result = load()
    result.reportLoadingProblemsIfAny { _, message -> error(message) }
    return result.librariesStdlibFirst.single()
}
