/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.config.exportedLibraries
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import java.nio.file.Path

internal fun ModuleDescriptor.getExportedDependencies(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries((config.exportedLibraries + config.loadedKlibs.included).toSet())

internal fun ModuleDescriptor.getIncludedLibraryDescriptors(config: NativeSecondStageCompilationConfig): List<ModuleDescriptor> =
        getDescriptorsFromLibraries(config.loadedKlibs.included.toSet())

private fun ModuleDescriptor.getDescriptorsFromLibraries(libraries: Set<KotlinLibrary>) =
    allDependencyModules.filter {
        when (val origin = it.klibModuleOrigin) {
            CurrentKlibModuleOrigin, SyntheticModulesOrigin -> false
            is DeserializedKlibModuleOrigin -> origin.library in libraries
        }
    }

internal fun getExportedLibraries(
    configuration: CompilerConfiguration,
    resolvedLibraries: KotlinLibraryResolveResult,
    resolver: SearchPathResolver<KotlinLibrary>,
): List<KotlinLibrary> = getFeaturedLibraries(
        configuration.exportedLibraries,
        resolvedLibraries,
        resolver,
        FeaturedLibrariesReporter.forExportedLibraries(configuration),
)

private sealed class FeaturedLibrariesReporter {

    abstract fun reportIllegalKind(library: KotlinLibrary)
    abstract fun reportNotIncludedLibraries(includedLibraries: List<KotlinLibrary>, remainingFeaturedLibraries: Set<Path>)

    protected val KotlinLibrary.reportedKind: String
        get() = when {
            isCInteropLibrary() -> "Interop"
            isFromKotlinNativeDistribution -> "Default"
            else -> "Unknown kind"
        }

    abstract class BaseReporter(val configuration: CompilerConfiguration) : FeaturedLibrariesReporter() {
        protected abstract fun illegalKindMessage(kind: String, libraryName: String): String
        protected abstract fun notIncludedLibraryMessageTitle(): String

        override fun reportIllegalKind(library: KotlinLibrary) {
            configuration.report(
                    CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING,
                    illegalKindMessage(library.reportedKind, library.path.toString())
            )
        }

        override fun reportNotIncludedLibraries(includedLibraries: List<KotlinLibrary>, remainingFeaturedLibraries: Set<Path>) {
            val message = buildString {
                appendLine(notIncludedLibraryMessageTitle())
                remainingFeaturedLibraries.forEach { appendLine(it) }
                appendLine()
                appendLine("Included libraries:")
                includedLibraries.forEach { appendLine(it.path) }
            }

            configuration.report(CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING, message)
        }
    }

    private class ExportedLibrariesReporter(configuration: CompilerConfiguration) : BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "$kind library $libraryName can't be exported with -Xexport-library"

        override fun notIncludedLibraryMessageTitle(): String =
            "Following libraries are specified to be exported with -Xexport-library, but not included to the build:"
    }

    companion object {
        fun forExportedLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter =
                ExportedLibrariesReporter(configuration)
    }
}

private fun getFeaturedLibraries(
        featuredLibraries: List<String>,
        resolvedLibraries: KotlinLibraryResolveResult,
        resolver: SearchPathResolver<KotlinLibrary>,
        reporter: FeaturedLibrariesReporter,
) = getFeaturedLibraries(
        featuredLibraries.toUnresolvedLibraries.map { resolver.resolve(it).path }.toSet(),
        resolvedLibraries,
        reporter,
)

private fun getFeaturedLibraries(
        featuredLibraryPaths: Set<Path>,
        resolvedLibraries: KotlinLibraryResolveResult,
        reporter: FeaturedLibrariesReporter,
) : List<KotlinLibrary> {
    val remainingFeaturedLibraries = featuredLibraryPaths.toMutableSet()
    val result = mutableListOf<KotlinLibrary>()
    //TODO: please add type checks before cast.
    val libraries = resolvedLibraries.getFullList()

    for (library in libraries) {
        val libraryPath = library.path
        if (libraryPath in featuredLibraryPaths) {
            remainingFeaturedLibraries.remove(libraryPath)
            if (library.isCInteropLibrary() || library.isFromKotlinNativeDistribution) {
                reporter.reportIllegalKind(library)
            } else {
                result += library
            }
        }
    }

    if (remainingFeaturedLibraries.isNotEmpty()) {
        reporter.reportNotIncludedLibraries(libraries, remainingFeaturedLibraries)
    }

    return result
}
