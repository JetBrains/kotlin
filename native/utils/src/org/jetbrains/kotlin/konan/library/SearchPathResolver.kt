package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.library.impl.createKonanLibrary
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
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
    skipCurrentDir: Boolean = false,
    zipFileSystemAccessor: ZipFileSystemAccessor? = null,
): SearchPathResolverWithTarget<KotlinLibrary> = KonanLibraryProperResolver(
    directLibs = directLibs,
    target = target,
    distributionKlib = distribution.klib,
    skipCurrentDir = skipCurrentDir,
    logger = logger,
    zipFileSystemAccessor,
)

class KonanLibraryProperResolver(
    directLibs: List<String>,
    override val target: KonanTarget,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger,
    val zipFileSystemAccessor: ZipFileSystemAccessor? = null,
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = skipCurrentDir,
    logger = logger,
    knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
), SearchPathResolverWithTarget<KotlinLibrary> {
    override fun loadLibrary(file: File, isDefault: Boolean) = createKonanLibrary(
        libraryFilePossiblyDenormalized = file,
        component = KLIB_DEFAULT_COMPONENT_NAME,
        target = target,
        isDefault = isDefault,
        zipFileSystemAccessor = zipFileSystemAccessor
    )

    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)

    override fun libraryMatch(candidate: KotlinLibrary, unresolved: UnresolvedLibrary): Boolean {
        val resolverTarget = this.target
        val candidatePath = candidate.libraryFile.absolutePath

        val supportedTargets = candidate.supportedTargetList
        if (supportedTargets.isNotEmpty()) {
            // TODO: We have a choice: either assume it is the CURRENT TARGET or a list of ALL KNOWN targets.
            if (resolverTarget.visibleName !in supportedTargets) {
                logger.strongWarning("KLIB resolver: Skipping '$candidatePath'. The target doesn't match. Expected '$resolverTarget', found $supportedTargets.")
                return false
            }
        }

        return super.libraryMatch(candidate, unresolved)
    }
}

