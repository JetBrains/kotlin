/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.repository

import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.cli.CliLoggerAdapter
import org.jetbrains.kotlin.commonizer.cli.errorAndExitJvmProcess
import org.jetbrains.kotlin.commonizer.konan.NativeLibrary
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.utils.mapToSetOrEmpty

internal class KonanDistributionRepository(
    konanDistribution: KonanDistribution,
    targets: Set<KonanTarget>,
    logger: CliLoggerAdapter,
) : Repository {
    private val librariesByTarget: Map<KonanTarget, Lazy<Set<NativeLibrary>>> =
        targets.associateWith { target ->
            lazy {
                val klibLoaderResult = KlibLoader {
                    libraryProviders(
                        KlibNativeDistributionLibraryProvider(konanDistribution.root.toPath().toFile()) {
                            withPlatformLibs(target)
                        }
                    )
                }.load()

                klibLoaderResult.reportLoadingProblemsIfAny { _, message -> logger.errorAndExitJvmProcess(message) }

                klibLoaderResult.librariesStdlibFirst.mapToSetOrEmpty { library ->
                    if (library.versions.metadataVersion == null)
                        logger.errorAndExitJvmProcess("Library does not have metadata version specified in manifest: ${library.path}")

                    NativeLibrary(library)
                }
            }
        }

    override fun getLibraries(target: CommonizerTarget): Set<NativeLibrary> {
        val singleTarget = target.konanTargets.singleOrNull() ?: return emptySet()
        return librariesByTarget[singleTarget]?.value ?: error("Missing target libraries for $target")
    }
}
