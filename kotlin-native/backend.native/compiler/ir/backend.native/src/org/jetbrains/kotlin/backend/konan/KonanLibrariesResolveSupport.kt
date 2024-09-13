/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.library.validateNoLibrariesWerePassedViaCliByUniqueName

class KonanLibrariesResolveSupport(
        configuration: CompilerConfiguration,
        target: KonanTarget,
        distribution: Distribution,
        resolveManifestDependenciesLenient: Boolean
) {
    private val includedLibraryFiles =
            configuration.getList(KonanConfigKeys.INCLUDED_LIBRARIES).map { File(it) }

    private val libraryToCacheFile =
                    configuration.get(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE)?.let { File(it) }

    private val libraryPaths = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val unresolvedLibraries = libraryPaths.toUnresolvedLibraries

    private val resolver = defaultResolver(
        libraryPaths + includedLibraryFiles.map { it.absolutePath },
        target,
        distribution,
        configuration.getLogger()
    ).libraryResolver(resolveManifestDependenciesLenient)

    // We pass included libraries by absolute paths to avoid repository-based resolution for them.
    // Strictly speaking such "direct" libraries should be specially handled by the resolver, not by KonanConfig.
    // But currently the resolver is in the middle of a complex refactoring so it was decided to avoid changes in its logic.
    // TODO: Handle included libraries in KonanLibraryResolver when it's refactored and moved into the big Kotlin repo.
    internal val resolvedLibraries = run {
        val additionalLibraryFiles = (includedLibraryFiles + listOfNotNull(libraryToCacheFile)).toSet()
        resolver.resolveWithDependencies(
            unresolvedLibraries + additionalLibraryFiles.map { RequiredUnresolvedLibrary(it.absolutePath) },
            noStdLib = configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
            noDefaultLibs = configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS),
            noEndorsedLibs = configuration.getBoolean(KonanConfigKeys.NOENDORSEDLIBS),
            duplicatedUniqueNameStrategy = configuration.get(
                KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                DuplicatedUniqueNameStrategy.DENY
            ),
        ).also { resolvedLibraries ->
            validateNoLibrariesWerePassedViaCliByUniqueName(libraryPaths, resolvedLibraries.getFullList(), resolver.logger)
        }
    }

    internal val exportedLibraries =
            getExportedLibraries(configuration, resolvedLibraries, resolver.searchPathResolver, report = true)

    internal val includedLibraries =
            getIncludedLibraries(includedLibraryFiles, configuration, resolvedLibraries)
}
