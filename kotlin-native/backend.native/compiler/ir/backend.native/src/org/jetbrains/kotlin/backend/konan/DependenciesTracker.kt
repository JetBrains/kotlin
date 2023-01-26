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

interface DependenciesTracker {
    sealed class DependencyKind {
        object WholeModule : DependencyKind()
        class CertainFiles(val files: List<String>) : DependencyKind()
    }

    data class UnresolvedDependency(val libName: String, val kind: DependencyKind) {
        companion object {
            fun wholeModule(libName: String) = UnresolvedDependency(libName, DependencyKind.WholeModule)
            fun certainFiles(libName: String, files: List<String>) = UnresolvedDependency(libName, DependencyKind.CertainFiles(files))
        }
    }

    data class ResolvedDependency(val library: KonanLibrary, val kind: DependencyKind) {
        companion object {
            fun wholeModule(library: KonanLibrary) = ResolvedDependency(library, DependencyKind.WholeModule)
            fun certainFiles(library: KonanLibrary, files: List<String>) = ResolvedDependency(library, DependencyKind.CertainFiles(files))
        }
    }

    fun add(irFile: IrFile, onlyBitcode: Boolean = false)
    fun add(declaration: IrDeclaration, onlyBitcode: Boolean = false)
    fun addNativeRuntime(onlyBitcode: Boolean = false)
    fun add(functionOrigin: FunctionOrigin, onlyBitcode: Boolean = false)

    val immediateBitcodeDependencies: List<ResolvedDependency>
    val allCachedBitcodeDependencies: List<ResolvedDependency>
    val allBitcodeDependencies: List<ResolvedDependency>
    val nativeDependenciesToLink: List<KonanLibrary>
    val allNativeDependencies: List<KonanLibrary>
    val bitcodeToLink: List<KonanLibrary>

    fun collectResult(): DependenciesTrackingResult
}

private sealed class FileOrigin {
    object CurrentFile : FileOrigin() // No dependency should be added.

    object StdlibRuntime : FileOrigin()

    object StdlibKFunctionImpl : FileOrigin()

    class EntireModule(val library: KotlinLibrary) : FileOrigin()

    class CertainFile(val library: KotlinLibrary, val fqName: String, val filePath: String) : FileOrigin()
}

internal class DependenciesTrackerImpl(
        private val llvmModuleSpecification: LlvmModuleSpecification,
        private val config: KonanConfig,
        private val context: Context,
) : DependenciesTracker {
    private data class LibraryFile(val library: KotlinLibrary, val fqName: String, val filePath: String)

    private val usedBitcode = mutableSetOf<KotlinLibrary>()
    private val usedNativeDependencies = mutableSetOf<KotlinLibrary>()
    private val usedBitcodeOfFile = mutableSetOf<LibraryFile>()

    private val allLibraries by lazy { context.config.librariesWithDependencies().toSet() }

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

    override fun add(functionOrigin: FunctionOrigin, onlyBitcode: Boolean) = when (functionOrigin) {
        FunctionOrigin.FromNativeRuntime -> addNativeRuntime(onlyBitcode)
        is FunctionOrigin.OwnedBy -> add(functionOrigin.declaration, onlyBitcode)
    }

    override fun add(irFile: IrFile, onlyBitcode: Boolean): Unit =
            add(computeFileOrigin(irFile) { irFile.path }, onlyBitcode)

    override fun add(declaration: IrDeclaration, onlyBitcode: Boolean): Unit =
            add(computeFileOrigin(declaration.getPackageFragment()) {
                context.irLinker.getExternalDeclarationFileName(declaration)
            }, onlyBitcode)

    override fun addNativeRuntime(onlyBitcode: Boolean) =
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
            isNewDependency = usedNativeDependencies.add(library) || isNewDependency
        }

        libraryFile?.let {
            isNewDependency = usedBitcodeOfFile.add(it) || isNewDependency
        }

        require(!(sealed && isNewDependency)) { "The dependencies have been sealed off" }
    }

    private fun bitcodeIsUsed(library: KonanLibrary) = library in usedBitcode

    private fun usedBitcode(): List<LibraryFile> = usedBitcodeOfFile.toList()

    private val topSortedLibraries by lazy {
        context.config.resolvedLibraries.getFullList(TopologicalLibraryOrder).map { it as KonanLibrary }
    }

    private inner class CachedBitcodeDependenciesComputer {
        private val allLibraries = topSortedLibraries.associateBy { it.uniqueName }
        private val usedBitcode = usedBitcode().groupBy { it.library }

        private val moduleDependencies = mutableSetOf<KonanLibrary>()
        private val fileDependencies = mutableMapOf<KonanLibrary, MutableSet<String>>()

        val allDependencies: List<DependenciesTracker.ResolvedDependency>

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

            allDependencies = moduleDependencies.map { DependenciesTracker.ResolvedDependency.wholeModule(it) } +
                    fileDependencies.filterNot { it.key in moduleDependencies }
                            .map { (library, files) -> DependenciesTracker.ResolvedDependency.certainFiles(library, files.toList()) }
        }

        private fun resolveDependency(dependency: DependenciesTracker.UnresolvedDependency) =
                DependenciesTracker.ResolvedDependency(
                        allLibraries[dependency.libName] ?: error("Unknown library: ${dependency.libName}"),
                        dependency.kind)

        private fun addAllDependencies(cachedLibrary: CachedLibraries.Cache) {
            cachedLibrary.bitcodeDependencies
                    .map { resolveDependency(it) }
                    .forEach { addDependency(it) }
        }

        private fun addDependencies(cachedLibrary: CachedLibraries.Cache, files: List<String>) = when (cachedLibrary) {
            is CachedLibraries.Cache.Monolithic -> addAllDependencies(cachedLibrary)

            is CachedLibraries.Cache.PerFile ->
                files.forEach { file ->
                    cachedLibrary.getFileDependencies(file)
                            .map { resolveDependency(it) }
                            .forEach { addDependency(it) }
                }
        }

        private fun addDependency(dependency: DependenciesTracker.ResolvedDependency) {
            val (library, kind) = dependency
            if (library in moduleDependencies) return
            val cachedDependency = context.config.cachedLibraries.getLibraryCache(library)
                    ?: error("Library ${library.libraryName} is expected to be cached")

            when (kind) {
                is DependenciesTracker.DependencyKind.WholeModule -> {
                    moduleDependencies.add(library)
                    addAllDependencies(cachedDependency)
                }
                is DependenciesTracker.DependencyKind.CertainFiles -> {
                    val handledFiles = fileDependencies.getOrPut(library) { mutableSetOf() }
                    val notHandledFiles = kind.files.toMutableSet()
                    notHandledFiles.removeAll(handledFiles)
                    handledFiles.addAll(notHandledFiles)
                    if (notHandledFiles.isNotEmpty())
                        addDependencies(cachedDependency, notHandledFiles.toList())
                }
            }
        }
    }

    private inner class Dependencies {
        val immediateBitcodeDependencies = run {
            val usedBitcode = usedBitcode().groupBy { it.library }
            val bitcodeModuleDependencies = mutableListOf<DependenciesTracker.ResolvedDependency>()
            val bitcodeFileDependencies = mutableListOf<DependenciesTracker.ResolvedDependency>()
            val libraryToCache = config.cacheSupport.libraryToCache
            val strategy = libraryToCache?.strategy as? CacheDeserializationStrategy.SingleFile
            topSortedLibraries.forEach { library ->
                val filesUsed = usedBitcode[library]
                if (filesUsed == null && bitcodeIsUsed(library) && library != libraryToCache?.klib /* Skip loops */) {
                    // Dependency on the entire library.
                    bitcodeModuleDependencies.add(DependenciesTracker.ResolvedDependency.wholeModule(library))
                }
                filesUsed?.filter { library != libraryToCache?.klib || strategy?.filePath != it.filePath /* Skip loops */ }
                        ?.map { CacheSupport.cacheFileId(it.fqName, it.filePath) }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { bitcodeFileDependencies.add(DependenciesTracker.ResolvedDependency.certainFiles(library, it)) }
            }
            bitcodeModuleDependencies + bitcodeFileDependencies
        }

        val allCachedBitcodeDependencies = CachedBitcodeDependenciesComputer().allDependencies

        val allBitcodeDependencies: List<DependenciesTracker.ResolvedDependency> = run {
            val allBitcodeDependencies = mutableMapOf<KonanLibrary, DependenciesTracker.ResolvedDependency>()
            for (library in context.config.librariesWithDependencies()) {
                if (context.config.cachedLibraries.getLibraryCache(library) == null || library == context.config.libraryToCache?.klib)
                    allBitcodeDependencies[library] = DependenciesTracker.ResolvedDependency.wholeModule(library)
            }
            for (dependency in allCachedBitcodeDependencies)
                allBitcodeDependencies[dependency.library] = dependency
            // This list is used in particular to build the libraries' initializers chain.
            // The initializers must be called in the topological order, so make sure that the
            // libraries list being returned is also toposorted.
            topSortedLibraries.mapNotNull { allBitcodeDependencies[it] }
        }

        val nativeDependenciesToLink = topSortedLibraries.filter { (!it.isDefault && !context.config.purgeUserLibs) || it in usedNativeDependencies }

        val allNativeDependencies = (nativeDependenciesToLink +
                allCachedBitcodeDependencies.map { it.library } // Native dependencies are per library
                ).distinct()

        val bitcodeToLink = topSortedLibraries.filter { shouldContainBitcode(it) }

        private fun shouldContainBitcode(library: KonanLibrary): Boolean {
            if (!llvmModuleSpecification.containsLibrary(library)) {
                return false
            }

            if (!llvmModuleSpecification.isFinal) {
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

    override val immediateBitcodeDependencies get() = dependencies.immediateBitcodeDependencies
    override val allCachedBitcodeDependencies get() = dependencies.allCachedBitcodeDependencies
    override val allBitcodeDependencies get() = dependencies.allBitcodeDependencies
    override val nativeDependenciesToLink get() = dependencies.nativeDependenciesToLink
    override val allNativeDependencies get() = dependencies.allNativeDependencies
    override val bitcodeToLink get() = dependencies.bitcodeToLink

    override fun collectResult(): DependenciesTrackingResult = DependenciesTrackingResult(
            bitcodeToLink,
            allNativeDependencies,
            allCachedBitcodeDependencies,
    )
}

internal object DependenciesSerializer {
    fun serialize(dependencies: List<DependenciesTracker.ResolvedDependency>) =
            dependencies.flatMap { (library, kind) ->
                val libName = library.uniqueName
                when (kind) {
                    DependenciesTracker.DependencyKind.WholeModule -> listOf("$libName$DEPENDENCIES_DELIMITER")
                    is DependenciesTracker.DependencyKind.CertainFiles -> kind.files.map { "$libName$DEPENDENCIES_DELIMITER$it" }
                }
            }

    fun deserialize(path: String, dependencies: List<String>): List<DependenciesTracker.UnresolvedDependency> {
        val wholeModuleDependencies = mutableListOf<String>()
        val fileDependencies = mutableMapOf<String, MutableList<String>>()
        for (dependency in dependencies) {
            val delimiterIndex = dependency.lastIndexOf(DEPENDENCIES_DELIMITER)
            require(delimiterIndex >= 0) { "Invalid dependency $dependency at $path" }
            val libName = dependency.substring(0, delimiterIndex)
            val file = dependency.substring(delimiterIndex + 1, dependency.length)
            if (file.isEmpty())
                wholeModuleDependencies.add(libName)
            else
                fileDependencies.getOrPut(libName) { mutableListOf() }.add(file)
        }
        return wholeModuleDependencies.map { DependenciesTracker.UnresolvedDependency.wholeModule(it) } +
                fileDependencies.map { (libName, files) -> DependenciesTracker.UnresolvedDependency.certainFiles(libName, files) }
    }

    private const val DEPENDENCIES_DELIMITER = '|'
}

/**
 * Result of dependency tracking during LLVM module production.
 * Elements of this class should be easily serializable/deserializable,
 * so the late compiler phases could be easily executed separately.
 */
data class DependenciesTrackingResult(
        val nativeDependenciesToLink: List<KonanLibrary>,
        val allNativeDependencies: List<KonanLibrary>,
        val allCachedBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>
)