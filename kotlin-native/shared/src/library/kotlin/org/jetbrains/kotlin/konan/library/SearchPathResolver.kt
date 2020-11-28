package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryImpl
import org.jetbrains.kotlin.konan.library.impl.createKonanLibraryComponents
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

interface SearchPathResolverWithTarget<L: KotlinLibrary>: SearchPathResolver<L> {
    val target: KonanTarget
}

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget,
        distribution: Distribution
): SearchPathResolverWithTarget<KonanLibrary> = defaultResolver(repositories, emptyList(), target, distribution)

fun defaultResolver(
    repositories: List<String>,
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget<KonanLibrary> = KonanLibraryProperResolver(
        repositories,
        directLibs,
        target,
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir,
        logger
)

fun resolverByName(
    repositories: List<String>,
    directLibs: List<String> = emptyList(),
    distributionKlib: String? = null,
    localKotlinDir: String? = null,
    skipCurrentDir: Boolean = false,
    logger: Logger
): SearchPathResolver<KotlinLibrary> =
    object : KotlinLibrarySearchPathResolver<KotlinLibrary>(
        repositories,
        directLibs,
        distributionKlib,
        localKotlinDir,
        skipCurrentDir,
        logger
    ) {
        override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKonanLibraryComponents(file, null, isDefault)
    }

internal class KonanLibraryProperResolver(
    repositories: List<String>,
    directLibs: List<String>,
    override val target: KonanTarget,
    distributionKlib: String?,
    localKonanDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KonanLibrary>(
    repositories, directLibs,
    distributionKlib,
    localKonanDir,
    skipCurrentDir,
    logger,
    listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
),  SearchPathResolverWithTarget<KonanLibrary>
{
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKonanLibraryComponents(file, target, isDefault)

    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)

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

