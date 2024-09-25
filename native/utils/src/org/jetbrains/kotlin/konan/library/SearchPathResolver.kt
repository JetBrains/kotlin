package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.createKonanLibraryComponents
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger

interface SearchPathResolverWithTarget<L : KotlinLibrary> : SearchPathResolver<L> {
    val target: KonanTarget
}

fun defaultResolver(
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget<KonanLibrary> = KonanLibraryProperResolver(
    directLibs = directLibs,
    target = target,
    distributionKlib = distribution.klib,
    skipCurrentDir = skipCurrentDir,
    logger = logger
)

class KonanLibraryProperResolver(
    directLibs: List<String>,
    override val target: KonanTarget,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KonanLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = skipCurrentDir,
    logger = logger,
    knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
), SearchPathResolverWithTarget<KonanLibrary> {
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKonanLibraryComponents(file, target, isDefault)

    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)

    override fun libraryMatch(candidate: KonanLibrary, unresolved: UnresolvedLibrary): Boolean {
        val resolverTarget = this.target
        val candidatePath = candidate.libraryFile.absolutePath

        if (!candidate.targetList.contains(resolverTarget.visibleName)) {
            logger.strongWarning("KLIB resolver: Skipping '$candidatePath'. The target doesn't match. Expected '$resolverTarget', found ${candidate.targetList}.")
            return false
        }

        return super.libraryMatch(candidate, unresolved)
    }
}

