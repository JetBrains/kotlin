/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary

internal fun Map<KotlinLibrary, ModuleDescriptor>.getExportedDependencies(
        config: NativeSecondStageCompilationConfig,
): List<ModuleDescriptor> =
        getDescriptorsFromLibraries(config.exportedLibraries())

/**
 * Same as [getExportedDependencies], but in reverse topological order: a module always precedes the modules
 * that depend on it, whatever order the libraries were passed in.
 *
 * * In what sense this is canonical
 * [org.jetbrains.kotlin.backend.konan.library.KlibDAG.librariesReverseTopoSorted] is a total order computed
 * from exactly two inputs: the dependency graph, and `LoadedNativeKlibs.all` (the order the libraries were
 * loaded in, which decides everything the edges leave unconstrained). Both inputs are explainable and
 * reproducible, and nothing else can reach the result. Note the load order is not merely preserved among
 * unrelated libraries: a library still waiting for its own dependencies can be overtaken by a later one that
 * is already ready.
 * So the order is canonical for a given graph and a given library list. It is NOT invariant under permuting
 * that list: two libraries that no dependency edge relates still swap if the CLI passes them in the other
 * order.
 */
internal fun Map<KotlinLibrary, ModuleDescriptor>.getExportedDependenciesInCanonicalRTO(
        config: NativeSecondStageCompilationConfig,
): List<ModuleDescriptor> {
    val exportedLibraries = config.exportedLibraries()
    return entries
            .filter { it.key in exportedLibraries }
            .sortedBy { config.klibDag.libraryReverseTopoRank.getValue(it.key) }
            .map { it.value }
}

internal fun Map<KotlinLibrary, ModuleDescriptor>.getIncludedLibraryDescriptors(
        config: NativeSecondStageCompilationConfig,
): List<ModuleDescriptor> =
        getDescriptorsFromLibraries(config.loadedKlibs.included.toSet())

private fun Map<KotlinLibrary, ModuleDescriptor>.getDescriptorsFromLibraries(
        libraries: Set<KotlinLibrary>,
): List<ModuleDescriptor> =
        filterKeys { it in libraries }.values.toList()

private fun NativeSecondStageCompilationConfig.exportedLibraries(): Set<KotlinLibrary> =
        (loadedKlibs.exported + loadedKlibs.included).toSet()
