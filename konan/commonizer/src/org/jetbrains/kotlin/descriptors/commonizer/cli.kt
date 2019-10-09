/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.commonizer.ModuleForCommonization.DeserializedModule
import org.jetbrains.kotlin.descriptors.commonizer.ModuleForCommonization.SyntheticModule
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.library.KonanFactories.DefaultDeserializedDescriptorFactory
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BaseWriterImpl
import org.jetbrains.kotlin.library.impl.KoltinLibraryWriterImpl
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.konan.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import kotlin.system.exitProcess
import org.jetbrains.kotlin.konan.file.File as KFile

fun main(args: Array<String>) {
    if (args.isEmpty()) printUsageAndExit()

    val parsedArgs = parseArgs(args)

    val repository = parsedArgs["-repository"]?.firstOrNull()?.let(::File) ?: printUsageAndExit("repository not specified")
    if (!repository.isDirectory) printErrorAndExit("repository does not exist: $repository")

    val targets = with(HostManager()) {
        val targetNames = parsedArgs["-target"]?.toSet() ?: printUsageAndExit("no targets specified")
        targetNames.map { targetName ->
            targets[targetName] ?: printUsageAndExit("unknown target name: $targetName")
        }
    }

    val destination = parsedArgs["-output"]?.firstOrNull()?.let(::File) ?: printUsageAndExit("output not specified")
    when {
        !destination.exists() -> destination.mkdirs()
        !destination.isDirectory -> printErrorAndExit("output already exists: $destination")
        destination.walkTopDown().any { it != destination } -> printErrorAndExit("output is not empty: $destination")
    }

    val modulesByTargets = loadModules(repository, targets)
    val result = commonize(modulesByTargets)
    saveModules(modulesByTargets, destination, result)

    println("Done.")
    println()
}

private fun parseArgs(args: Array<String>): Map<String, List<String>> {
    val commandLine = mutableMapOf<String, MutableList<String>>()
    for (index in args.indices step 2) {
        val key = args[index]
        if (key[0] != '-') printUsageAndExit("Expected a flag with initial dash: $key")
        if (index + 1 == args.size) printUsageAndExit("Expected a value after $key")
        val value = args[index + 1]
        commandLine.computeIfAbsent(key) { mutableListOf() }.add(value)
    }
    return commandLine
}

private fun printErrorAndExit(errorMessage: String): Nothing {
    println("Error: $errorMessage")
    println()

    exitProcess(1)
}

private fun printUsageAndExit(errorMessage: String? = null): Nothing {
    if (errorMessage != null) {
        println("Error: $errorMessage")
        println()
    }

    println("Usage: commonizer <options>")
    println("where possible options include:")
    println("\t-repository <path>\tWork with the specified Kotlin/Native repository")
    println("\t-target <name>\t\tAdd hardware target to commonization")
    println("\t-output <path>\t\tDestination of commonized KLIBs")
    println()

    exitProcess(if (errorMessage != null) 1 else 0)
}

private fun loadModules(repository: File, targets: List<KonanTarget>): Map<InputTarget, List<ModuleForCommonization>> {
    val stdlibPath = repository.resolve(konanCommonLibraryPath(KONAN_STDLIB_NAME))
    val stdlib = loadLibrary(stdlibPath)

    val librariesByTargets = targets.map { target ->
        val platformLibsPath = repository.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
            .resolve(target.name)

        val platformLibs = platformLibsPath.takeIf { it.isDirectory }
            ?.listFiles()
            ?.takeIf { it.isNotEmpty() }
            ?.map { loadLibrary(it) }
            ?: printErrorAndExit("no platform libraries found for target $target in $platformLibsPath")

        InputTarget(target.name, target) to platformLibs
    }.toMap()

    return librariesByTargets.mapValues { (target, libraries) ->
        val storageManager = LockBasedStorageManager("Target $target")

        val rawStdlibModule = DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library = stdlib,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            storageManager = storageManager,
            packageAccessHandler = null
        )

        val otherModules = libraries.map { library ->
            val rawModule = DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                library = library,
                languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
                storageManager = storageManager,
                builtIns = rawStdlibModule.builtIns,
                packageAccessHandler = null
            )
            val data = SensitiveManifestData.readFrom(library)
            DeserializedModule(rawModule, data, File(library.libraryFile.path))
        }

        val rawForwardDeclarationsModule = createKotlinNativeForwardDeclarationsModule(
            storageManager = storageManager,
            builtIns = rawStdlibModule.builtIns
        )

        val onlyDeserializedModules = listOf(rawStdlibModule) + otherModules.map { it.module }
        val allModules = onlyDeserializedModules + rawForwardDeclarationsModule
        onlyDeserializedModules.forEach { it.setDependencies(allModules) }

        val stdlibModule = DeserializedModule(rawStdlibModule, SensitiveManifestData.readFrom(stdlib), File(stdlib.libraryFile.path))
        val forwardDeclarationsModule = SyntheticModule(rawForwardDeclarationsModule)

        listOf(stdlibModule) + otherModules + forwardDeclarationsModule
    }
}

private fun loadLibrary(location: File): KotlinLibrary {
    if (!location.isDirectory) printErrorAndExit("library not found: $location")
    return createKotlinLibrary(KFile(location.path))
}

private fun commonize(modulesByTargets: Map<InputTarget, List<ModuleForCommonization>>): CommonizationPerformed {
    val parameters = CommonizationParameters().apply {
        modulesByTargets.forEach { (target, modules) ->
            addTarget(target, modules.map { it.module })
        }
    }

    val result = runCommonization(parameters)
    return when (result) {
        is NothingToCommonize -> printUsageAndExit("too few targets specified: ${modulesByTargets.keys}")
        is CommonizationPerformed -> result
    }
}

private fun saveModules(
    originalModulesByTargets: Map<InputTarget, List<ModuleForCommonization>>,
    destination: File,
    result: CommonizationPerformed
) {
    // optimization: stdlib effectively remains the same across all Kotlin/Native targets,
    // so it can be just copied to the new destination without running serializer
    val stdlibOrigin = originalModulesByTargets.values.asSequence()
        .flatten()
        .filterIsInstance<DeserializedModule>()
        .map { it.location }
        .first { it.endsWith(KONAN_STDLIB_NAME) }

    val stdlibDestination = destination.resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR).resolve(KONAN_STDLIB_NAME)
    stdlibOrigin.copyRecursively(stdlibDestination)

    val originalModulesManifestData = originalModulesByTargets.mapValues { (_, modules) ->
        modules.asSequence()
            .filterIsInstance<DeserializedModule>()
            .filter { !it.location.endsWith(KONAN_STDLIB_NAME) }
            .associate { it.module.name to it.data }
    }

    val stdlibName = Name.special("<$KONAN_STDLIB_NAME>")

    result.concreteTargets.forEach { target ->
        val konanTarget = target.konanTarget!!
        val targetLibsDestination = destination.resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR).resolve(konanTarget.name)

        val newModulesManifestData = originalModulesManifestData.getValue(target)
        val newModules = result.modulesByTargets.getValue(target)

        for (newModule in newModules) {
            val libraryName = newModule.name
            if (libraryName == stdlibName || libraryName == FORWARD_DECLARATIONS_MODULE_NAME) continue

            // TODO: obtain serialization data
//            val metadata = TODO()
            val manifestData = newModulesManifestData.getValue(newModule.name)
            val libraryDestination = targetLibsDestination.resolve(libraryName.asString().trimStart('<').trimEnd('>'))

//            writeLibrary(metadata, manifestData, libraryDestination)
            println("$manifestData - $libraryDestination")
        }
    }

    TODO("serialize common target")
}

private sealed class ModuleForCommonization(val module: ModuleDescriptorImpl) {
    class DeserializedModule(
        module: ModuleDescriptorImpl,
        val data: SensitiveManifestData,
        val location: File
    ) : ModuleForCommonization(module)

    class SyntheticModule(module: ModuleDescriptorImpl) : ModuleForCommonization(module)
}

private data class SensitiveManifestData(
    val uniqueName: String,
    val versions: KotlinLibraryVersioning,
    val dependencies: List<String>,
    val isInterop: Boolean,
    val packageFqName: String?,
    val exportForwardDeclarations: List<String>
) {
    fun applyTo(library: BaseWriterImpl) {
        library.manifestProperties[KLIB_PROPERTY_UNIQUE_NAME] = uniqueName

        // note: versions can't be added here

        if (dependencies.isNotEmpty())
            library.manifestProperties[KLIB_PROPERTY_DEPENDS] = dependencies.joinToString(separator = " ")
        else
            library.manifestProperties.remove(KLIB_PROPERTY_DEPENDS)

        if (isInterop)
            library.manifestProperties[KLIB_PROPERTY_INTEROP] = "true"
        else
            library.manifestProperties.remove(KLIB_PROPERTY_INTEROP)

        if (packageFqName != null)
            library.manifestProperties[KLIB_PROPERTY_PACKAGE] = packageFqName
        else
            library.manifestProperties.remove(KLIB_PROPERTY_PACKAGE)

        if (exportForwardDeclarations.isNotEmpty())
            library.manifestProperties[KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS] =
                exportForwardDeclarations.joinToString(separator = " ")
        else
            library.manifestProperties.remove(KLIB_PROPERTY_EXPORT_FORWARD_DECLARATIONS)
    }

    companion object {
        fun readFrom(library: KotlinLibrary) = SensitiveManifestData(
            uniqueName = library.uniqueName,
            versions = library.versions,
            dependencies = library.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true),
            isInterop = library.isInterop,
            packageFqName = library.packageFqName,
            exportForwardDeclarations = library.exportForwardDeclarations
        )
    }
}

private fun writeLibrary(
    metadata: SerializedMetadata,
    manifestData: SensitiveManifestData,
    destination: File
) {
    val library = KoltinLibraryWriterImpl(KFile(destination.path), manifestData.uniqueName, manifestData.versions, nopack = false)
    library.addMetadata(metadata)
    manifestData.applyTo(library.base as BaseWriterImpl)
    library.commit()
}
