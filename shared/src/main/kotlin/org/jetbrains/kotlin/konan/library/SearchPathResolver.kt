package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.removeSuffixIfPresent
import org.jetbrains.kotlin.konan.util.suffixIfNot

const val KLIB_FILE_EXTENSION = "klib"
const val KLIB_FILE_EXTENSION_WITH_DOT = ".$KLIB_FILE_EXTENSION"

const val KONAN_STDLIB_NAME = "stdlib"

interface SearchPathResolver {
    val searchRoots: List<File>
    fun resolve(givenPath: String): File
    fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<File>
}

interface SearchPathResolverWithTarget: SearchPathResolver {
    val target: KonanTarget
}

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget
): SearchPathResolverWithTarget = defaultResolver(repositories, target, Distribution())

fun defaultResolver(
        repositories: List<String>,
        target: KonanTarget,
        distribution: Distribution,
        skipCurrentDir: Boolean = false
): SearchPathResolverWithTarget = KonanLibrarySearchPathResolverWithTarget(
        repositories,
        target,
        distribution.klib,
        distribution.localKonanDir.absolutePath,
        skipCurrentDir)

fun noTargetResolver(
        repositories: List<String>,
        distributionKlib: String? = null,
        localKonanDir: String? = null,
        skipCurrentDir: Boolean = false
): SearchPathResolver = KonanLibrarySearchPathResolver(repositories, distributionKlib, localKonanDir, skipCurrentDir)


internal open class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        val distributionKlib: String?,
        val localKonanDir: String?,
        val skipCurrentDir: Boolean
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

    override fun resolve(givenPath: String): File {
        val given = File(givenPath)
        if (given.isAbsolute) {
            found(given)?.apply { return this }
        } else {
            searchRoots.forEach {
                found(File(it, givenPath))?.apply { return this }
            }
        }
        error("Could not find \"$givenPath\" in ${searchRoots.map { it.absolutePath }}.")
    }

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOfNotNull(distHead, distPlatformHead).filter { it.exists }

    override fun defaultLinks(noStdLib: Boolean, noDefaultLibs: Boolean): List<File> {

        val result = mutableListOf<File>()

        if (!noStdLib) {
            result.add(resolve(KONAN_STDLIB_NAME))
        }

        if (!noDefaultLibs) {
            val defaultLibs = defaultRoots.flatMap { it.listFiles }
                    .filterNot { it.name.removeSuffixIfPresent(KLIB_FILE_EXTENSION_WITH_DOT) == KONAN_STDLIB_NAME }
                    .map { File(it.absolutePath) }
            result.addAll(defaultLibs)
        }

        return result
    }
}

internal class KonanLibrarySearchPathResolverWithTarget(
        repositories: List<String>,
        override val target: KonanTarget,
        distributionKlib: String?,
        localKonanDir: String?,
        skipCurrentDir: Boolean
): KonanLibrarySearchPathResolver(repositories, distributionKlib, localKonanDir, skipCurrentDir), SearchPathResolverWithTarget {

    override val distPlatformHead: File?
        get() = distributionKlib?.File()?.child("platform")?.child(target.visibleName)
}