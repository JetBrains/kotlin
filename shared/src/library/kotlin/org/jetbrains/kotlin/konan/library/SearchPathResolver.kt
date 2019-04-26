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
        target: KonanTarget,
        distribution: Distribution = Distribution(),
        compatibleCompilerVersions: List<KonanVersion>
): SearchPathResolverWithTarget = defaultResolver(repositories, emptyList(), target, distribution, compatibleCompilerVersions)

fun defaultResolver(
        repositories: List<String>,
        directLibs: List<String>,
        target: KonanTarget,
        distribution: Distribution,
        compatibleCompilerVersions: List<KonanVersion>,
        logger: Logger = ::dummyLogger,
        skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget = KonanLibraryProperResolver(
        repositories,
        directLibs,
        target,
        listOf(KonanAbiVersion.CURRENT),
        compatibleCompilerVersions,
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir,
        logger)

fun resolverByName(
        repositories: List<String>,
        directLibs: List<String> = emptyList(),
        distributionKlib: String? = null,
        localKonanDir: String? = null,
        skipCurrentDir: Boolean = false
): SearchPathResolver = KonanLibrarySearchPathResolver(repositories, directLibs, distributionKlib, localKonanDir, skipCurrentDir)

internal open class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        directLibs: List<String>,
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

    private val directLibraries: List<KonanLibrary> by lazy {
        directLibs.mapNotNull { found(File(it)) }.map { createKonanLibrary(it, null) }
    }

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
            // Search among user-provided libraries by unique name.
            // It's a workaround for maven publication. When a library is published without Gradle metadata,
            // it has a complex file name (e.g. foo-macos_x64-1.0.klib). But a dependency on this lib in manifests
            // of other libs uses its unique name written in the manifest (i.e just 'foo'). So we cannot resolve this
            // library by its filename. But we have this library's file (we've downloaded it using maven dependency
            // resolution) so we can pass it to the compiler directly. This code takes this into account and looks for
            // a library dependencies also in libs passed to the compiler as files (passed to the resolver as the
            // 'directLibraries' property).
            val directLibs = directLibraries.asSequence().filter {
                it.uniqueName == givenPath
            }.map {
                it.libraryFile
            }
            // Search among libraries in repositoreis by library filename.
            val repoLibs = searchRoots.asSequence().map {
                found(File(it, givenPath))
            }
            directLibs + repoLibs
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
                    .asSequence()
                    .filterNot { it.name.startsWith('.') }
                    .filterNot { it.name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT) == KONAN_STDLIB_NAME }
                    .map { UnresolvedLibrary(it.absolutePath, null) }
                    .map { resolve(it, isDefaultLink = true) }
            result.addAll(defaultLibs)
        }

        return result
    }
}

internal class KonanLibraryProperResolver(
    repositories: List<String>,
    directLibs: List<String>,
    override val target: KonanTarget,
    override val knownAbiVersions: List<KonanAbiVersion>?,
    override val knownCompilerVersions: List<KonanVersion>?,
    distributionKlib: String?,
    localKonanDir: String?,
    skipCurrentDir: Boolean,
    override val logger: Logger = ::dummyLogger
) : KonanLibrarySearchPathResolver(repositories, directLibs, distributionKlib, localKonanDir, skipCurrentDir, logger),
    SearchPathResolverWithTarget
{
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

    val candidateCompilerVersion = candidate.versions.compilerVersion
    val candidateAbiVersion = candidate.versions.abiVersion
    val candidateLibraryVersion = candidate.versions.libraryVersion

    if (!candidate.targetList.contains(resolverTarget.visibleName)) {
        logger("skipping $candidatePath. The target doesn't match. Expected '$resolverTarget', found ${candidate.targetList}")
        return false
    }

    val abiVersionMatch = candidateAbiVersion != null &&
            knownAbiVersions != null &&
            knownAbiVersions!!.contains(candidateAbiVersion)

    val compilerVersionMatch = candidateCompilerVersion != null &&
            knownCompilerVersions != null &&
            knownCompilerVersions!!.any { it.compatible(candidateCompilerVersion) }

    if (!abiVersionMatch && !compilerVersionMatch) {
        logger("skipping $candidatePath. The abi versions don't match. Expected '${knownAbiVersions}', found '${candidateAbiVersion}'")

        if (knownCompilerVersions != null) {
            val expected = knownCompilerVersions?.map { it.toString(false, false) }
            val found = candidateCompilerVersion?.toString(true, true)
            logger("The compiler versions don't match either. Expected '${expected}', found '${found}'")
        }

        return false
    }

    if (candidateLibraryVersion != unresolved.libraryVersion &&
            candidateLibraryVersion != null &&
            unresolved.libraryVersion != null) {
        logger("skipping $candidatePath. The library versions don't match. Expected '${unresolved.libraryVersion}', found '${candidateLibraryVersion}'")
        return false
    }

    return true
}

private fun KonanVersion.compatible(other: KonanVersion) =
        this.major == other.major
        && this.minor == other.minor
        && this.maintenance == other.maintenance
