package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryImpl
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

const val KONAN_STDLIB_NAME = "stdlib"

interface SearchPathResolverWithTarget<L: KotlinLibrary>: SearchPathResolverWithAttributes<L> {
    val target: KonanTarget
}

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget,
        distribution: Distribution = Distribution(),
        compatibleCompilerVersions: List<KonanVersion> = emptyList()
): SearchPathResolverWithTarget<KonanLibrary> = defaultResolver(repositories, emptyList(), target, distribution, compatibleCompilerVersions)

fun defaultResolver(
    repositories: List<String>,
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    compatibleCompilerVersions: List<KonanVersion> = emptyList(),
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget<KonanLibrary> = KonanLibraryProperResolver(
        repositories,
        directLibs,
        target,
        listOf(KotlinAbiVersion.CURRENT),
        compatibleCompilerVersions,
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir,
        logger
)

internal class KonanLibraryProperResolver(
    repositories: List<String>,
    directLibs: List<String>,
    override val target: KonanTarget,
    knownAbiVersions: List<KotlinAbiVersion>?,
    knownCompilerVersions: List<KonanVersion>?,
    distributionKlib: String?,
    localKonanDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KonanLibrary>(
    repositories, directLibs,
    knownAbiVersions,
    knownCompilerVersions,
    distributionKlib,
    localKonanDir,
    skipCurrentDir,
    logger,
    {file: File, isDefaultLink: Boolean -> createKonanLibrary(file, target, isDefaultLink)}
),  SearchPathResolverWithTarget<KonanLibrary>
{
    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)

    override fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): KonanLibraryImpl {
        val givenPath = unresolved.path
        val fileSequence = resolutionSequence(givenPath)
        val matching = fileSequence.map { createKonanLibrary(it, target, isDefaultLink) as KonanLibraryImpl }
                .map { it.takeIf { libraryMatch(it, unresolved) } }
                .filterNotNull()

        return matching.firstOrNull() ?: run {
            logger.fatal("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
        }
    }

    override fun libraryMatch(candidate: KonanLibrary, unresolved: UnresolvedLibrary): Boolean {
        val resolverTarget = this.target
        val candidatePath = candidate.libraryFile.absolutePath

        if (!candidate.targetList.contains(resolverTarget.visibleName)) {
            logger.warning("skipping $candidatePath. The target doesn't match. Expected '$resolverTarget', found ${candidate.targetList}")
            return false
        }

        return super.libraryMatch(candidate, unresolved)
    }
}

