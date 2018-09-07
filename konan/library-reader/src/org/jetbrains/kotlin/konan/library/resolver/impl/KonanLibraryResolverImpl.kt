/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.resolver.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.SearchPathResolverWithTarget
import org.jetbrains.kotlin.konan.library.createKonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.*
import org.jetbrains.kotlin.konan.library.unresolvedDependencies

internal class KonanLibraryResolverImpl(
    override val searchPathResolver: SearchPathResolverWithTarget,
    override val abiVersion: Int
) : KonanLibraryResolver {

    override fun resolveWithDependencies(
        libraryNames: List<String>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean,
        logger: DuplicatedLibraryLogger?
    ) = findLibraries(libraryNames, noStdLib, noDefaultLibs)
        .leaveDistinct(logger)
        .resolveDependencies()

    /**
     * Returns the list of libraries based on [libraryNames], [noStdLib] and [noDefaultLibs] criteria.
     *
     * This method does not return any libraries that might be available via transitive dependencies
     * from the original library set (root set).
     */
    private fun findLibraries(
        libraryNames: List<String>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean
    ): List<KonanLibrary> {

        val userProvidedLibraries = libraryNames.asSequence()
            .map { searchPathResolver.resolve(it) }
            .map { createKonanLibrary(it, abiVersion, searchPathResolver.target) }
            .toList()

        val defaultLibraries = searchPathResolver.defaultLinks(noStdLib, noDefaultLibs).map {
            createKonanLibrary(it, abiVersion, searchPathResolver.target, isDefault = true)
        }

        // Make sure the user provided ones appear first, so that
        // they have precedence over defaults when duplicates are eliminated.
        return userProvidedLibraries + defaultLibraries
    }

    /**
     * Leaves only distinct libraries (by absolute path), warns on duplicated paths.
     */
    private fun List<KonanLibrary>.leaveDistinct(logger: DuplicatedLibraryLogger?) =
        this.groupBy { it.libraryFile.absolutePath }.let { groupedByAbsolutePath ->
            warnOnLibraryDuplicates(groupedByAbsolutePath.filter { it.value.size > 1 }.keys, logger)
            groupedByAbsolutePath.map { it.value.first() }
        }

    private fun warnOnLibraryDuplicates(duplicatedPaths: Iterable<String>, logger: DuplicatedLibraryLogger?) {
        if (logger == null) return
        duplicatedPaths.forEach { logger("library included more than once: $it") }
    }


    /**
     * Given the list of root libraries does the following:
     *
     * 1. Evaluates other libraries that are available via transitive dependencies.
     * 2. Wraps each [KonanLibrary] into a [KonanResolvedLibrary] with information about dependencies on other libraries.
     * 3. Creates resulting [KonanLibraryResolveResult] object.
     */
    private fun List<KonanLibrary>.resolveDependencies(): KonanLibraryResolveResult {

        val rootLibraries = this.map { KonanResolvedLibraryImpl(it) }

        // As far as the list of root libraries is known from the very beginning, the result can be
        // constructed from the very beginning as well.
        val result = KonanLibraryResolverResultImpl(rootLibraries)

        val cache = mutableMapOf<File, KonanResolvedLibrary>()
        cache.putAll(rootLibraries.map { it.library.libraryFile.absoluteFile to it })

        var newDependencies = rootLibraries
        do {
            newDependencies = newDependencies.map { library: KonanResolvedLibraryImpl ->
                library.library.unresolvedDependencies.asSequence()
                    .map { searchPathResolver.resolve(it).absoluteFile }
                    .mapNotNull {
                        if (it in cache) {
                            library.addDependency(cache[it]!!)
                            null
                        } else {
                            val newLibrary = KonanResolvedLibraryImpl(createKonanLibrary(it, abiVersion, searchPathResolver.target))
                            cache[it] = newLibrary
                            library.addDependency(newLibrary)
                            newLibrary
                        }
                    }
                    .toList()
            }.flatten()
        } while (newDependencies.isNotEmpty())

        return result
    }
}

internal class KonanLibraryResolverResultImpl(
    private val roots: List<KonanResolvedLibrary>
) : KonanLibraryResolveResult {

    private val all: List<KonanResolvedLibrary> by lazy {
        val result = mutableSetOf<KonanResolvedLibrary>().also { it.addAll(roots) }

        var newDependencies = result.toList()
        do {
            newDependencies = newDependencies
                .map { it -> it.resolvedDependencies }.flatten()
                .filter { it !in result }
            result.addAll(newDependencies)
        } while (newDependencies.isNotEmpty())

        result.toList()
    }

    override fun filterRoots(predicate: (KonanResolvedLibrary) -> Boolean) =
        KonanLibraryResolverResultImpl(roots.filter(predicate))

    override fun getFullList(order: LibraryOrder?) = (order?.invoke(all) ?: all).asPlain()

    override fun forEach(action: (KonanLibrary, PackageAccessedHandler) -> Unit) {
        all.forEach { action(it.library, it) }
    }

    private fun List<KonanResolvedLibrary>.asPlain() = map { it.library }

    override fun toString() = "roots=$roots, all=$all"
}
