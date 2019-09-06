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

interface SearchPathResolverWithTarget<out L: KotlinLibrary>: SearchPathResolverWithAttributes<L> {
    val target: KonanTarget
}

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget,
        distribution: Distribution = Distribution(),
        compatibleCompilerVersions: List<KonanVersion> = emptyList()
): SearchPathResolverWithTarget<KonanLibraryImpl> = defaultResolver(repositories, emptyList(), target, distribution, compatibleCompilerVersions)

fun defaultResolver(
    repositories: List<String>,
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    compatibleCompilerVersions: List<KonanVersion> = emptyList(),
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget<KonanLibraryImpl> = KonanLibraryProperResolver(
        repositories,
        directLibs,
        target,
        listOf(KotlinAbiVersion.CURRENT),
        compatibleCompilerVersions,
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir,
        logger)

internal class KonanLibraryProperResolver(
    repositories: List<String>,
    directLibs: List<String>,
    override val target: KonanTarget,
    override val knownAbiVersions: List<KotlinAbiVersion>?,
    override val knownCompilerVersions: List<KonanVersion>?,
    distributionKlib: String?,
    localKonanDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger = DummyLogger
) : KotlinLibrarySearchPathResolver<KonanLibraryImpl>(repositories, directLibs, distributionKlib, localKonanDir, skipCurrentDir, logger),
    SearchPathResolverWithTarget<KonanLibraryImpl>
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
}

internal fun SearchPathResolverWithTarget<KonanLibraryImpl>.libraryMatch(candidate: KonanLibraryImpl, unresolved: UnresolvedLibrary): Boolean {
    val resolverTarget = this.target
    val candidatePath = candidate.libraryFile.absolutePath

    val candidateCompilerVersion = candidate.versions.compilerVersion
    val candidateAbiVersion = candidate.versions.abiVersion
    val candidateLibraryVersion = candidate.versions.libraryVersion

    if (!candidate.targetList.contains(resolverTarget.visibleName)) {
        logger.warning("skipping $candidatePath. The target doesn't match. Expected '$resolverTarget', found ${candidate.targetList}")
        return false
    }

    val abiVersionMatch = candidateAbiVersion != null &&
            knownAbiVersions != null &&
            knownAbiVersions!!.contains(candidateAbiVersion)

    val compilerVersionMatch = candidateCompilerVersion != null &&
            knownCompilerVersions != null &&
            knownCompilerVersions!!.any { it.compatible(candidateCompilerVersion) }

    if (!abiVersionMatch && !compilerVersionMatch) {
        logger.warning("skipping $candidatePath. The abi versions don't match. Expected '${knownAbiVersions}', found '${candidateAbiVersion}'")

        if (knownCompilerVersions != null) {
            val expected = knownCompilerVersions?.map { it.toString(false, false) }
            val found = candidateCompilerVersion?.toString(true, true)
            logger.warning("The compiler versions don't match either. Expected '${expected}', found '${found}'")
        }

        return false
    }

    if (candidateLibraryVersion != unresolved.libraryVersion &&
            candidateLibraryVersion != null &&
            unresolved.libraryVersion != null) {
        logger.warning("skipping $candidatePath. The library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidateLibraryVersion}'")
        return false
    }

    return true
}
