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

@Deprecated(
    "Please use defaultNativeKlibResolver() instead, which has 'skipNativeCommonLibs' and 'skipNativePlatformLibs' parameters",
    ReplaceWith("defaultNativeKlibResolver(directLibs, target, distribution, logger, skipCurrentDir, skipNativeCommonLibs = false, skipNativePlatformLibs = false)")
)
fun defaultResolver(
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget<KonanLibrary> = defaultNativeKlibResolver(
    directLibs = directLibs,
    target = target,
    distribution = distribution,
    skipCurrentDir = skipCurrentDir,
    skipNativeCommonLibs = false,
    skipNativePlatformLibs = false,
    logger = logger
)

fun defaultNativeKlibResolver(
    directLibs: List<String>,
    target: KonanTarget,
    distribution: Distribution,
    logger: Logger = DummyLogger,
    skipCurrentDir: Boolean = false,
    skipNativeCommonLibs: Boolean = false,
    skipNativePlatformLibs: Boolean = false,
): SearchPathResolverWithTarget<KonanLibrary> = KonanLibraryProperResolver(
    directLibs = directLibs,
    target = target,
    distributionKlib = distribution.klib,
    skipCurrentDir = skipCurrentDir,
    skipNativeCommonLibs = skipNativeCommonLibs,
    skipNativePlatformLibs = skipNativePlatformLibs,
    logger = logger
)

class KonanLibraryProperResolver(
    directLibs: List<String>,
    override val target: KonanTarget,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    skipNativeCommonLibs: Boolean,
    skipNativePlatformLibs: Boolean,
    override val logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KonanLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = skipCurrentDir,
    skipNativeCommonLibs = skipNativeCommonLibs,
    logger = logger,
    knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
), SearchPathResolverWithTarget<KonanLibrary> {
    @Deprecated(
        "Please use the primary constructor instead, which has 'skipNativeCommonLibs' and 'skipNativePlatformLibs' parameters",
        ReplaceWith("KonanLibraryProperResolver>(directLibs, target, distributionKlib, skipCurrentDir, skipNativeCommonLibs = false, skipNativePlatformLibs = false, logger)"),
    )
    constructor(
        directLibs: List<String>,
        target: KonanTarget,
        distributionKlib: String?,
        skipCurrentDir: Boolean,
        logger: Logger
    ) : this(
        directLibs = directLibs,
        target = target,
        distributionKlib = distributionKlib,
        skipCurrentDir = skipCurrentDir,
        skipNativeCommonLibs = false,
        skipNativePlatformLibs = false,
        logger = logger,
    )

    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKonanLibraryComponents(file, target, isDefault)

    override val nativeDistPlatformLibsDir: File? by lazy {
        distributionKlib?.takeUnless { skipNativePlatformLibs }?.File()?.child("platform")?.child(target.visibleName)
    }

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

