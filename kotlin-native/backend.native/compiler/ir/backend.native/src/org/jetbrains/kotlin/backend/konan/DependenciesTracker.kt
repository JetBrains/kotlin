/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.legacyKlibReverseTopoSort
import org.jetbrains.kotlin.utils.atMostOne
import org.jetbrains.kotlin.backend.konan.llvm.FunctionOrigin
import org.jetbrains.kotlin.backend.konan.llvm.llvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.llvm.standardLlvmSymbolsOrigin
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.CachedEagerInitializedFiles
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

interface DependenciesTracker {
    data class FileDependency(val name: String, val weak: Boolean)

    sealed class DependencyKind {
        object WholeModule : DependencyKind()
        class CertainFiles(val files: List<FileDependency>) : DependencyKind()
    }

    data class UnresolvedDependency(val libName: String, val kind: DependencyKind) {
        companion object {
            fun wholeModule(libName: String) = UnresolvedDependency(libName, DependencyKind.WholeModule)
            fun certainFiles(libName: String, files: List<FileDependency>) = UnresolvedDependency(libName, DependencyKind.CertainFiles(files))
        }
    }

    data class ResolvedDependency(val library: KotlinLibrary, val kind: DependencyKind) {
        companion object {
            fun wholeModule(library: KotlinLibrary) = ResolvedDependency(library, DependencyKind.WholeModule)
            fun certainFiles(library: KotlinLibrary, files: List<FileDependency>) = ResolvedDependency(library, DependencyKind.CertainFiles(files))
        }
    }

    fun add(library: KotlinLibrary, onlyBitcode: Boolean = false)
    fun add(irFile: IrFile, weak: Boolean = false, onlyBitcode: Boolean = false)
    fun add(declaration: IrDeclaration, weak: Boolean = false, onlyBitcode: Boolean = false)
    fun addNativeRuntime(onlyBitcode: Boolean = false)
    fun add(functionOrigin: FunctionOrigin, onlyBitcode: Boolean = false)

    val immediateBitcodeDependencies: List<ResolvedDependency>
    val allCachedBitcodeDependencies: List<ResolvedDependency>
    val allBitcodeDependencies: List<ResolvedDependency>
    val nativeDependenciesToLink: List<KotlinLibrary>
    val allNativeDependencies: List<KotlinLibrary>
    val bitcodeToLink: List<KotlinLibrary>

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
        private val config: NativeSecondStageCompilationConfig,
        private val context: Context,
) : DependenciesTracker {
    private data class LibraryFile(val library: KotlinLibrary, val fqName: String, val filePath: String)

    private val usedBitcode = mutableSetOf<KotlinLibrary>()
    private val usedNativeDependencies = mutableSetOf<KotlinLibrary>()
    private val usedBitcodeOfFile = mutableSetOf<LibraryFile>()
    private val usedWeakBitcodeOfFile = mutableSetOf<LibraryFile>()

    private val allLibraries by lazy { context.config.librariesWithDependencies().toSet() }

    private fun findStdlibFile(fqName: FqName, fileName: String): LibraryFile {
        val stdlib = (context.standardLlvmSymbolsOrigin as? DeserializedKlibModuleOrigin)?.library
                ?: error("Can't find stdlib")
        val stdlibDeserializer = context.moduleDeserializerProvider.getDeserializerOrNull(stdlib)
                ?: error("No deserializer for stdlib")
        val file = stdlibDeserializer.files.atMostOne { it.packageFqName == fqName && it.name == fileName }
                ?: error("Can't find $fqName:$fileName in stdlib")
        return LibraryFile(stdlib, file.packageFqName.asString(), file.path)
    }

    private val stdlibRuntime by lazy { findStdlibFile(KonanFqNames.internalPackageName, "Runtime.kt") }
    private val stdlibKFunctionImpl by lazy { findStdlibFile(KonanFqNames.internalPackageName, "KFunctionImpl.kt") }

    private var sealed = false

    override fun add(library: KotlinLibrary, onlyBitcode: Boolean) {
        add(FileOrigin.EntireModule(library), weak = false, onlyBitcode)
    }

    override fun add(functionOrigin: FunctionOrigin, onlyBitcode: Boolean) = when (functionOrigin) {
        FunctionOrigin.FromNativeRuntime -> addNativeRuntime(onlyBitcode)
        is FunctionOrigin.OwnedBy -> add(functionOrigin.declaration, functionOrigin.weak, onlyBitcode)
    }

    override fun add(irFile: IrFile, weak: Boolean, onlyBitcode: Boolean): Unit =
            add(computeFileOrigin(irFile) { irFile.path }, weak, onlyBitcode)

    override fun add(declaration: IrDeclaration, weak: Boolean, onlyBitcode: Boolean): Unit =
            add(computeFileOrigin(declaration.getPackageFragment()) {
                context.externalDeclarationFileNameProvider.getExternalDeclarationFileName(declaration)
            }, weak, onlyBitcode)

    override fun addNativeRuntime(onlyBitcode: Boolean) =
            add(FileOrigin.StdlibRuntime, weak = false, onlyBitcode)

    private fun computeFileOrigin(packageFragment: IrPackageFragment, filePathGetter: () -> String): FileOrigin {
        return if (packageFragment.isFunctionInterfaceFile)
            FileOrigin.StdlibKFunctionImpl
        else {
            val library = when (val origin = packageFragment.llvmSymbolOrigin) {
                CurrentKlibModuleOrigin -> config.libraryToCache?.klib?.takeIf { config.producePerFileCache }
                else -> (origin as DeserializedKlibModuleOrigin).library
            }
            when {
                library == null -> FileOrigin.CurrentFile
                library.isCInteropLibrary() -> FileOrigin.EntireModule(library)
                else -> FileOrigin.CertainFile(library, packageFragment.packageFqName.asString(), filePathGetter())
            }
        }
    }

    private fun add(origin: FileOrigin, weak: Boolean, onlyBitcode: Boolean = false) {
        val libraryFile = when (origin) {
            FileOrigin.CurrentFile -> return
            is FileOrigin.EntireModule -> null
            is FileOrigin.CertainFile -> LibraryFile(origin.library, origin.fqName, origin.filePath)
            FileOrigin.StdlibRuntime -> stdlibRuntime
            FileOrigin.StdlibKFunctionImpl -> stdlibKFunctionImpl
        }
        val library = libraryFile?.library ?: (origin as FileOrigin.EntireModule).library
        if (library !in allLibraries)
            error("Library (${library.location}) is used but not requested.\nRequested libraries: ${allLibraries.joinToString { it.location.path }}")

        var isNewDependency = usedBitcode.add(library)
        if (!onlyBitcode) {
            isNewDependency = usedNativeDependencies.add(library) || isNewDependency
        }

        libraryFile?.let {
            isNewDependency = (if (weak) usedWeakBitcodeOfFile.add(it) else usedBitcodeOfFile.add(it)) || isNewDependency
        }

        require(!(sealed && isNewDependency)) { "The dependencies have been sealed off" }
    }

    private fun bitcodeIsUsed(library: KotlinLibrary) = library in usedBitcode

    private data class UsedLibraryFile(val file: LibraryFile, val weak: Boolean)

    private fun usedBitcode(): List<UsedLibraryFile> =
            usedBitcodeOfFile.map { UsedLibraryFile(it, weak = false) } +
                    usedWeakBitcodeOfFile.map { UsedLibraryFile(it, weak = true) }

    private val topSortedLibraries by lazy {
        context.config.resolvedLibraries.getFullList().legacyKlibReverseTopoSort()
    }

    private inner class CachedBitcodeDependenciesComputer {
        private val allLibraries = topSortedLibraries.associateBy { it.uniqueName }
        private val usedBitcode = usedBitcode().groupBy { it.file.library }

        private val moduleDependencies = mutableSetOf<KotlinLibrary>()
        private val fileDependencies = mutableMapOf<KotlinLibrary, MutableSet<DependenciesTracker.FileDependency>>()

        val allDependencies: List<DependenciesTracker.ResolvedDependency>

        init {
            val immediateBitcodeDependencies = topSortedLibraries
                    .filter { (!it.isFromKotlinNativeDistribution && !context.config.purgeUserLibs) || bitcodeIsUsed(it) }
            for (library in immediateBitcodeDependencies) {
                if (library == context.config.libraryToCache?.klib) continue
                val cache = context.config.cachedLibraries.getLibraryCache(library)

                if (cache != null) {
                    val filesUsed = buildList {
                        usedBitcode[library]?.forEach { (file, weak) ->
                            add(DependenciesTracker.FileDependency(
                                    CacheSupport.cacheFileId(file.fqName, file.filePath), weak
                            ))
                        }
                        val moduleDeserializer = context.moduleDeserializerProvider.getDeserializerOrNull(library)
                        if (moduleDeserializer == null) {
                            require(library.isCInteropLibrary()) { "No module deserializer for cached library ${library.uniqueName}" }
                        } else {
                            val eagerInitializedFiles = CachedEagerInitializedFiles(context.config.cachedLibraries, library, moduleDeserializer)
                                    .eagerInitializedFiles
                            eagerInitializedFiles.forEach {
                                add(DependenciesTracker.FileDependency(
                                        CacheSupport.cacheFileId(it.packageFqName.asString(), it.path), weak = false
                                ))
                            }
                        }
                    }

                    if (filesUsed.isEmpty() || library in config.includedLibraries) {
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
                            .map { (library, files) ->
                                DependenciesTracker.ResolvedDependency.certainFiles(library, files.toList().filterNot { (file, weak) ->
                                    weak && DependenciesTracker.FileDependency(file, weak = false) in files
                                })
                            }
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

        private fun addDependencies(cachedLibrary: CachedLibraries.Cache, files: List<DependenciesTracker.FileDependency>) = when (cachedLibrary) {
            is CachedLibraries.Cache.Monolithic -> addAllDependencies(cachedLibrary)

            is CachedLibraries.Cache.PerFile ->
                files.forEach { file ->
                    cachedLibrary.getFileDependencies(file.name)
                            .map { resolveDependency(it) }
                            .forEach { addDependency(it) }
                }
        }

        private fun addDependency(dependency: DependenciesTracker.ResolvedDependency) {
            val (library, kind) = dependency
            if (library in moduleDependencies) return
            val cachedDependency = context.config.cachedLibraries.getLibraryCache(library)
                    ?: error("Library ${library.location} is expected to be cached")

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
            val usedBitcode = usedBitcode().groupBy { it.file.library }
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
                filesUsed?.filter { library != libraryToCache?.klib || strategy?.filePath != it.file.filePath /* Skip loops */ }
                        ?.map { (file, weak) ->
                            DependenciesTracker.FileDependency(
                                    CacheSupport.cacheFileId(file.fqName, file.filePath), weak
                            )
                        }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { bitcodeFileDependencies.add(DependenciesTracker.ResolvedDependency.certainFiles(library, it)) }
            }
            bitcodeModuleDependencies + bitcodeFileDependencies
        }

        val allCachedBitcodeDependencies = CachedBitcodeDependenciesComputer().allDependencies

        val allBitcodeDependencies: List<DependenciesTracker.ResolvedDependency> = run {
            val allBitcodeDependencies = mutableMapOf<KotlinLibrary, DependenciesTracker.ResolvedDependency>()
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

        val nativeDependenciesToLink = topSortedLibraries.filter { (!it.isFromKotlinNativeDistribution && !context.config.purgeUserLibs) || it in usedNativeDependencies }

        val allNativeDependencies = (nativeDependenciesToLink +
                allCachedBitcodeDependencies.map { it.library } // Native dependencies are per library
                ).distinct()

        val bitcodeToLink = topSortedLibraries.filter { shouldContainBitcode(it) }

        private fun shouldContainBitcode(library: KotlinLibrary): Boolean {
            if (!llvmModuleSpecification.containsLibrary(library)) {
                return false
            }

            if (!llvmModuleSpecification.isFinal) {
                return true
            }

            // Apply some DCE:
            return (!library.isFromKotlinNativeDistribution && !context.config.purgeUserLibs) || bitcodeIsUsed(library)
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
                    is DependenciesTracker.DependencyKind.CertainFiles -> kind.files.map { (name, weak) ->
                        if (weak)
                            "$libName$DEPENDENCIES_DELIMITER$name$DEPENDENCY_KIND_DELIMITER$DEPENDENCY_WEAK"
                        else
                            "$libName$DEPENDENCIES_DELIMITER$name"
                    }
                }
            }

    fun deserialize(path: String, dependencies: List<String>): List<DependenciesTracker.UnresolvedDependency> {
        val wholeModuleDependencies = mutableListOf<String>()
        val fileDependencies = mutableMapOf<String, MutableList<DependenciesTracker.FileDependency>>()
        for (dependency in dependencies) {
            // <lib>| or <lib>|<file> or <lib>|<file>@weak
            val delimiterIndex = dependency.lastIndexOf(DEPENDENCIES_DELIMITER)
            require(delimiterIndex >= 0) { "Invalid dependency $dependency at $path" }
            val libName = dependency.substring(0, delimiterIndex)
            val file = dependency.substring(delimiterIndex + 1, dependency.length)
            if (file.isEmpty())
                wholeModuleDependencies.add(libName)
            else {
                val kindDelimiterIndex = file.lastIndexOf(DEPENDENCY_KIND_DELIMITER)
                if (kindDelimiterIndex < 0)
                    fileDependencies.getOrPut(libName) { mutableListOf() }.add(DependenciesTracker.FileDependency(file, weak = false))
                else {
                    val fileName = file.substring(0, kindDelimiterIndex)
                    val kind = file.substring(kindDelimiterIndex + 1, file.length).toLowerCaseAsciiOnly()
                    val weak = when (kind) {
                        DEPENDENCY_WEAK -> true
                        else -> error("Unexpected dependency kind $kind at $path for $dependency")
                    }
                    fileDependencies.getOrPut(libName) { mutableListOf() }.add(DependenciesTracker.FileDependency(fileName, weak))
                }
            }
        }
        return wholeModuleDependencies.map { DependenciesTracker.UnresolvedDependency.wholeModule(it) } +
                fileDependencies.map { (libName, files) -> DependenciesTracker.UnresolvedDependency.certainFiles(libName, files) }
    }

    private const val DEPENDENCIES_DELIMITER = '|'
    private const val DEPENDENCY_KIND_DELIMITER = '@'
    private const val DEPENDENCY_WEAK = "weak"
}

/**
 * Result of dependency tracking during LLVM module production.
 * Elements of this class should be easily serializable/deserializable,
 * so the late compiler phases could be easily executed separately.
 */
data class DependenciesTrackingResult(
        val nativeDependenciesToLink: List<KotlinLibrary>,
        val allNativeDependencies: List<KotlinLibrary>, // Used to get proper linker options (like linking a specific native library).
        val allCachedBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>) {

    companion object {
        private const val NATIVE_DEPENDENCIES_TO_LINK = "NATIVE_DEPENDENCIES_TO_LINK"
        private const val ALL_NATIVE_DEPENDENCIES = "ALL_NATIVE_DEPENDENCIES"
        private const val ALL_CACHED_BITCODE_DEPENDENCIES = "ALL_CACHED_BITCODE_DEPENDENCIES"

        fun serialize(res: DependenciesTrackingResult): List<String> {
            val nativeDepsToLink = DependenciesSerializer.serialize(res.nativeDependenciesToLink.map { DependenciesTracker.ResolvedDependency.wholeModule(it) })
            val allNativeDeps = DependenciesSerializer.serialize(res.allNativeDependencies.map { DependenciesTracker.ResolvedDependency.wholeModule(it) })
            val allCachedBitcodeDeps = DependenciesSerializer.serialize(res.allCachedBitcodeDependencies)
            return listOf(NATIVE_DEPENDENCIES_TO_LINK) + nativeDepsToLink +
                    listOf(ALL_NATIVE_DEPENDENCIES) + allNativeDeps +
                    listOf(ALL_CACHED_BITCODE_DEPENDENCIES) + allCachedBitcodeDeps
        }

        fun deserialize(path: String, dependencies: List<String>, config: NativeSecondStageCompilationConfig): DependenciesTrackingResult {

            val nativeDepsToLinkIndex = dependencies.indexOf(NATIVE_DEPENDENCIES_TO_LINK)
            require(nativeDepsToLinkIndex >= 0) { "Invalid dependency file at $path" }
            val allNativeDepsIndex = dependencies.indexOf(ALL_NATIVE_DEPENDENCIES)
            require(allNativeDepsIndex >= 0) { "Invalid dependency file at $path" }
            val allCachedBitcodeDepsIndex = dependencies.indexOf(ALL_CACHED_BITCODE_DEPENDENCIES)
            require(allCachedBitcodeDepsIndex >= 0) { "Invalid dependency file at $path" }

            val nativeLibsToLink = DependenciesSerializer.deserialize(path, dependencies.subList(nativeDepsToLinkIndex + 1, allNativeDepsIndex)).map { it.libName }
            val allNativeLibs = DependenciesSerializer.deserialize(path, dependencies.subList(allNativeDepsIndex + 1, allCachedBitcodeDepsIndex)).map { it.libName }
            val allCachedBitcodeDeps = DependenciesSerializer.deserialize(path, dependencies.subList(allCachedBitcodeDepsIndex + 1, dependencies.size))

            val topSortedLibraries = config.resolvedLibraries.getFullList().legacyKlibReverseTopoSort()
            val nativeDependenciesToLink = topSortedLibraries.mapNotNull { if (it.uniqueName in nativeLibsToLink) it else null }
            val allNativeDependencies = topSortedLibraries.mapNotNull { if (it.uniqueName in allNativeLibs) it else null }
            val allCachedBitcodeDependencies = allCachedBitcodeDeps.map { unresolvedDep ->
                val lib = topSortedLibraries.find { it.uniqueName == unresolvedDep.libName }
                require(lib != null) { "Invalid dependency ${unresolvedDep.libName} at $path" }
                when (unresolvedDep.kind) {
                    is DependenciesTracker.DependencyKind.CertainFiles ->
                        DependenciesTracker.ResolvedDependency.certainFiles(lib, unresolvedDep.kind.files)
                    else -> DependenciesTracker.ResolvedDependency.wholeModule(lib)
                }
            }

            return DependenciesTrackingResult(nativeDependenciesToLink, allNativeDependencies, allCachedBitcodeDependencies)
        }
    }
}
