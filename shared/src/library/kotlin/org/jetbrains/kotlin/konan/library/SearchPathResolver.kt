package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.KonanAbiVersion
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.KonanLibraryImpl
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.*

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove the constants below:
const val KONAN_STDLIB_NAME = "stdlib"

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this interface!
interface SearchPathResolver : WithLogger {
    val searchRoots: List<File>
    fun resolutionSequence(givenPath: String): Sequence<File>
    fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean = false): KonanLibraryImpl
    fun resolve(givenPath: String): KonanLibraryImpl
    fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<KonanLibraryImpl>
}

// FIXME(ddol): KLIB-REFACTORING-CLEANUP: remove this interface!
interface SearchPathResolverWithTarget: SearchPathResolver {
    val target: KonanTarget
    val knownAbiVersions: List<KonanAbiVersion>?
    val knownCompilerVersions: List<KonanVersion>?
}

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget
): SearchPathResolverWithTarget = defaultResolver(repositories, target, Distribution())

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget,
        distribution: Distribution,
        logger: Logger = ::dummyLogger,
        skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget = KonanLibraryProperResolver(
        repositories,
        target,
        listOf(KonanAbiVersion.CURRENT),
        listOf(KonanVersion.CURRENT),
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir,
        logger)

fun resolverByName(
        repositories: List<String>,
        distributionKlib: String? = null,
        localKonanDir: String? = null,
        skipCurrentDir: Boolean = false
): SearchPathResolver = KonanLibrarySearchPathResolver(repositories, distributionKlib, localKonanDir, skipCurrentDir)

internal open class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        val distributionKlib: String?,
        val localKonanDir: String?,
        val skipCurrentDir: Boolean,
        override val logger:Logger = ::dummyLogger
) : SearchPathResolver {

    val localHead: File?
        get() = localKonanDir?.File()?.klib

    val distHead: File?
        get() = distributionKlib?.File()?.child("common")

    open val distPlatformHead: File? = null

    val currentDirHead: File?
        get() = if (!skipCurrentDir) File.userDir else null

    private val repoRoots: List<File> by lazy { repositories.map { File(it) } }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        (listOf(currentDirHead) + repoRoots + listOf(localHead, distHead, distPlatformHead)).filterNotNull()
    }

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean =
                file.exists && (file.isFile || File(file, "manifest").exists)

        val noSuffix = File(candidate.path.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT))
        val withSuffix = File(candidate.path.suffixIfNot(KLIB_FILE_EXTENSION_WITH_DOT))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }

    override fun resolutionSequence(givenPath: String): Sequence<File> {
        val given = File(givenPath)
        val sequence = if (given.isAbsolute) {
            sequenceOf(found(given))
        } else {
            searchRoots.asSequence().map {
                found(File(it, givenPath))
            }
        }
        return sequence.filterNotNull()
    }

    override fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): KonanLibraryImpl {
        val givenPath = unresolved.path
        return resolutionSequence(givenPath).firstOrNull() ?. let {
            createKonanLibrary(it, null, isDefaultLink) as KonanLibraryImpl
        } ?: error("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
    }

    override fun resolve(givenPath: String) = resolve(UnresolvedLibrary(givenPath, null), false)


    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOfNotNull(distHead, distPlatformHead).filter { it.exists }

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<KonanLibraryImpl> {

        val result = mutableListOf<KonanLibraryImpl>()

        if (!noStdLib) {
            result.add(resolve(UnresolvedLibrary(KONAN_STDLIB_NAME, null), true))
        }

        if (!noDefaultLibs) {
            val defaultLibs = defaultRoots.flatMap { it.listFiles }
                    .filterNot { it.name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT) == KONAN_STDLIB_NAME }
                    .map { UnresolvedLibrary(it.absolutePath, null) }
                    .map {resolve(it, isDefaultLink = true) }
            result.addAll(defaultLibs)
        }

        return result
    }
}

internal class KonanLibraryProperResolver(
        repositories: List<String>,
        override val target: KonanTarget,
        override val knownAbiVersions: List<KonanAbiVersion>?,
        override val knownCompilerVersions: List<KonanVersion>?,
        distributionKlib: String?,
        localKonanDir: String?,
        skipCurrentDir: Boolean,
        override val logger: Logger = ::dummyLogger
): KonanLibrarySearchPathResolver(repositories, distributionKlib, localKonanDir, skipCurrentDir, logger), SearchPathResolverWithTarget {

    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)

    override fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): KonanLibraryImpl {
        val givenPath = unresolved.path
        val fileSequence = resolutionSequence(givenPath)
        val matching = fileSequence.map { createKonanLibrary(it, target, isDefaultLink) as KonanLibraryImpl }
                .map { it.takeIf { libraryMatch(it, unresolved) } }
                .filterNotNull()

        return matching.firstOrNull() ?: error("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
    }
}

internal fun SearchPathResolverWithTarget.libraryMatch(candidate: KonanLibraryImpl, unresolved: UnresolvedLibrary): Boolean {
    val resolverTarget = this.target
    val candidatePath = candidate.libraryFile.absolutePath

    if (resolverTarget != null && !candidate.targetList.contains(resolverTarget.visibleName)) {
        logger("skipping $candidatePath. The target doesn't match. Expected '$resolverTarget', found ${candidate.targetList}")
        return false
    }

    if (knownCompilerVersions != null &&
            !knownCompilerVersions!!.contains(candidate.versions.compilerVersion)) {
        logger("skipping $candidatePath. The compiler versions don't match. Expected '${knownCompilerVersions}', found '${candidate.versions.compilerVersion}'")
        return false
    }

    if (knownAbiVersions != null &&
            !knownAbiVersions!!.contains(candidate.versions.abiVersion)) {
        logger("skipping $candidatePath. The abi versions don't match. Expected '${knownAbiVersions}', found '${candidate.versions.abiVersion}'")
        return false
    }

    if (candidate.versions.libraryVersion != unresolved.libraryVersion &&
            candidate.versions.libraryVersion != null &&
            unresolved.libraryVersion != null) {
        logger("skipping $candidatePath. The library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidate.versions.libraryVersion}'")
        return false
    }

    return true
}
