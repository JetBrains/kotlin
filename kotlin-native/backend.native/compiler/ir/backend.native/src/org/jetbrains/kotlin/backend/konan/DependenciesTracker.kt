/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.llvm.FunctionOrigin
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.llvm.standardLlvmSymbolsOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.name.FqName

private sealed class FileOrigin {
    object CurrentFile : FileOrigin() // No dependency should be added.

    object StdlibRuntime : FileOrigin()

    object StdlibKFunctionImpl : FileOrigin()

    class EntireModule(val library: KotlinLibrary) : FileOrigin()

    class CertainFile(val library: KotlinLibrary, val fqName: String, val filePath: String) : FileOrigin()
}

internal class DependenciesTracker(private val generationState: NativeGenerationState) {
    data class LibraryFile(val library: KotlinLibrary, val fqName: String, val filePath: String)

    sealed class CachedBitcodeDependency(val library: KonanLibrary) {
        class WholeModule(library: KonanLibrary) : CachedBitcodeDependency(library)

        class CertainFiles(library: KonanLibrary, val files: List<String>) : CachedBitcodeDependency(library)
    }

    private val config = generationState.config
    private val context = generationState.context

    private val usedBitcode = mutableSetOf<KotlinLibrary>()
    private val usedNativeDependencies = mutableSetOf<KotlinLibrary>()
    private val usedBitcodeOfFile = mutableSetOf<LibraryFile>()

    private val allLibraries by lazy { context.librariesWithDependencies.toSet() }

    private fun findStdlibFile(fqName: FqName, fileName: String): LibraryFile {
        val stdlib = (context.standardLlvmSymbolsOrigin as? DeserializedKlibModuleOrigin)?.library
                ?: error("Can't find stdlib")
        val stdlibDeserializer = context.irLinker.moduleDeserializers[context.stdlibModule]
                ?: error("No deserializer for stdlib")
        val file = stdlibDeserializer.files.atMostOne { it.fqName == fqName && it.name == fileName }
                ?: error("Can't find $fqName:$fileName in stdlib")
        return LibraryFile(stdlib, file.fqName.asString(), file.path)
    }

    private val stdlibRuntime by lazy { findStdlibFile(KonanFqNames.internalPackageName, "Runtime.kt") }
    private val stdlibKFunctionImpl by lazy { findStdlibFile(KonanFqNames.internalPackageName, "KFunctionImpl.kt") }

    private var sealed = false

    fun add(functionOrigin: FunctionOrigin, onlyBitcode: Boolean = false) = when (functionOrigin) {
        FunctionOrigin.FromNativeRuntime -> addNativeRuntime(onlyBitcode)
        is FunctionOrigin.OwnedBy -> add(functionOrigin.declaration, onlyBitcode)
    }

    fun add(irFile: IrFile, onlyBitcode: Boolean = false): Unit =
            add(computeFileOrigin(irFile) { irFile.path }, onlyBitcode)

    fun add(declaration: IrDeclaration, onlyBitcode: Boolean = false): Unit =
            add(computeFileOrigin(declaration.getPackageFragment()) {
                context.irLinker.getExternalDeclarationFileName(declaration)
            }, onlyBitcode)

    fun addNativeRuntime(onlyBitcode: Boolean = false) =
            add(FileOrigin.StdlibRuntime, onlyBitcode)

    private fun computeFileOrigin(packageFragment: IrPackageFragment, filePathGetter: () -> String): FileOrigin {
        return if (packageFragment.isFunctionInterfaceFile)
            FileOrigin.StdlibKFunctionImpl
        else {
            val library = when (val origin = packageFragment.packageFragmentDescriptor.llvmSymbolOrigin) {
                CurrentKlibModuleOrigin -> config.libraryToCache?.klib?.takeIf { config.producePerFileCache }
                else -> (origin as DeserializedKlibModuleOrigin).library
            }
            when {
                library == null -> FileOrigin.CurrentFile
                packageFragment.packageFragmentDescriptor.containingDeclaration.isFromInteropLibrary() ->
                    FileOrigin.EntireModule(library)
                else -> FileOrigin.CertainFile(library, packageFragment.fqName.asString(), filePathGetter())
            }
        }
    }

    private fun add(origin: FileOrigin, onlyBitcode: Boolean = false) {
        val libraryFile = when (origin) {
            FileOrigin.CurrentFile -> return
            is FileOrigin.EntireModule -> null
            is FileOrigin.CertainFile -> LibraryFile(origin.library, origin.fqName, origin.filePath)
            FileOrigin.StdlibRuntime -> stdlibRuntime
            FileOrigin.StdlibKFunctionImpl -> stdlibKFunctionImpl
        }
        val library = libraryFile?.library ?: (origin as FileOrigin.EntireModule).library
        if (library !in allLibraries)
            error("Library (${library.libraryName}) is used but not requested.\nRequested libraries: ${allLibraries.joinToString { it.libraryName }}")

        var isNewDependency = usedBitcode.add(library)
        if (!onlyBitcode) {
            isNewDependency = isNewDependency || usedNativeDependencies.add(library)
        }

        libraryFile?.let {
            isNewDependency = isNewDependency || usedBitcodeOfFile.add(it)
        }

        require(!(sealed && isNewDependency)) { "The dependencies have been sealed off" }
    }

    fun bitcodeIsUsed(library: KonanLibrary) = library in usedBitcode

    fun usedBitcode(): List<LibraryFile> = usedBitcodeOfFile.toList()

    private val topSortedLibraries by lazy {
        context.config.resolvedLibraries.getFullList(TopologicalLibraryOrder).map { it as KonanLibrary }
    }

    private inner class CachedBitcodeDependenciesComputer {
        private val allLibraries = topSortedLibraries.associateBy { it.uniqueName }
        private val usedBitcode = usedBitcode().groupBy { it.library }

        private val moduleDependencies = mutableSetOf<KonanLibrary>()
        private val fileDependencies = mutableMapOf<KonanLibrary, MutableSet<String>>()

        val allDependencies: List<CachedBitcodeDependency>

        init {
            val immediateBitcodeDependencies = topSortedLibraries
                    .filter { (!it.isDefault && !context.config.purgeUserLibs) || bitcodeIsUsed(it) }
            val moduleDeserializers = context.irLinker.moduleDeserializers.values.associateBy { it.klib }
            for (library in immediateBitcodeDependencies) {
                if (library == context.config.libraryToCache?.klib) continue
                val cache = context.config.cachedLibraries.getLibraryCache(library)

                if (cache != null) {
                    val filesUsed = buildList {
                        usedBitcode[library]?.forEach {
                            add(CacheSupport.cacheFileId(it.fqName, it.filePath))
                        }
                        val moduleDeserializer = moduleDeserializers[library]
                        if (moduleDeserializer == null) {
                            require(library.isInteropLibrary()) { "No module deserializer for cached library ${library.uniqueName}" }
                        } else {
                            moduleDeserializer.eagerInitializedFiles.forEach {
                                add(CacheSupport.cacheFileId(it.fqName.asString(), it.path))
                            }
                        }
                    }

                    if (filesUsed.isEmpty()) {
                        // This is the case when we depend on the whole module rather than on a number of files.
                        moduleDependencies.add(library)
                        addAllDependencies(cache)
                    } else {
                        fileDependencies.getOrPut(library) { mutableSetOf() }.addAll(filesUsed)
                        addDependencies(cache, filesUsed)
                    }
                }
            }

            allDependencies = moduleDependencies.map { CachedBitcodeDependency.WholeModule(it) } +
                    fileDependencies.filterNot { it.key in moduleDependencies }
                            .map { (library, files) -> CachedBitcodeDependency.CertainFiles(library, files.toList()) }
        }

        private fun addAllDependencies(cachedLibrary: CachedLibraries.Cache) {
            cachedLibrary.bitcodeDependencies.forEach { addDependency(it) }
        }

        private fun addDependencies(cachedLibrary: CachedLibraries.Cache, files: List<String>) = when (cachedLibrary) {
            is CachedLibraries.Cache.Monolithic -> addAllDependencies(cachedLibrary)

            is CachedLibraries.Cache.PerFile ->
                files.forEach { file ->
                    cachedLibrary.getFileDependencies(file).forEach { addDependency(it) }
                }
        }

        private fun addDependency(dependency: CachedLibraries.BitcodeDependency) {
            val dependencyLib = allLibraries[dependency.libName]
                    ?: error("Bitcode dependency to an unknown library: ${dependency.libName}")
            if (dependencyLib in moduleDependencies) return
            val cachedDependency = context.config.cachedLibraries.getLibraryCache(dependencyLib)
                    ?: error("Library ${dependency.libName} is expected to be cached")

            when (dependency) {
                is CachedLibraries.BitcodeDependency.WholeModule -> {
                    moduleDependencies.add(dependencyLib)
                    addAllDependencies(cachedDependency)
                }
                is CachedLibraries.BitcodeDependency.CertainFiles -> {
                    val handledFiles = fileDependencies.getOrPut(dependencyLib) { mutableSetOf() }
                    val notHandledFiles = dependency.files.toMutableSet()
                    notHandledFiles.removeAll(handledFiles)
                    handledFiles.addAll(notHandledFiles)
                    if (notHandledFiles.isNotEmpty())
                        addDependencies(cachedDependency, notHandledFiles.toList())
                }
            }
        }
    }

    private inner class Dependencies {
        val allCachedBitcodeDependencies = CachedBitcodeDependenciesComputer().allDependencies

        val nativeDependenciesToLink = topSortedLibraries.filter { (!it.isDefault && !context.config.purgeUserLibs) || it in usedNativeDependencies }

        val allNativeDependencies = (nativeDependenciesToLink +
                allCachedBitcodeDependencies.map { it.library } // Native dependencies are per library
                ).distinct()

        val allBitcodeDependencies: List<CachedBitcodeDependency> = run {
            val allBitcodeDependencies = mutableMapOf<KonanLibrary, CachedBitcodeDependency>()
            for (library in context.librariesWithDependencies) {
                if (context.config.cachedLibraries.getLibraryCache(library) == null || library == context.config.libraryToCache?.klib)
                    allBitcodeDependencies[library] = CachedBitcodeDependency.WholeModule(library)
            }
            for (dependency in allCachedBitcodeDependencies)
                allBitcodeDependencies[dependency.library] = dependency
            // This list is used in particular to build the libraries' initializers chain.
            // The initializers must be called in the topological order, so make sure that the
            // libraries list being returned is also toposorted.
            topSortedLibraries.mapNotNull { allBitcodeDependencies[it] }
        }

        val bitcodeToLink = topSortedLibraries.filter { shouldContainBitcode(it) }

        private fun shouldContainBitcode(library: KonanLibrary): Boolean {
            if (!generationState.llvmModuleSpecification.containsLibrary(library)) {
                return false
            }

            if (!generationState.llvmModuleSpecification.isFinal) {
                return true
            }

            // Apply some DCE:
            return (!library.isDefault && !context.config.purgeUserLibs) || bitcodeIsUsed(library)
        }
    }

    private val dependencies by lazy {
        sealed = true

        Dependencies()
    }

    val allCachedBitcodeDependencies get() = dependencies.allCachedBitcodeDependencies
    val nativeDependenciesToLink get() = dependencies.nativeDependenciesToLink
    val allNativeDependencies get() = dependencies.allNativeDependencies
    val allBitcodeDependencies get() = dependencies.allBitcodeDependencies
    val bitcodeToLink get() = dependencies.bitcodeToLink
}