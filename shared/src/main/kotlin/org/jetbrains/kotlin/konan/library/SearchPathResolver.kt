package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.removeSuffixIfPresent
import org.jetbrains.kotlin.konan.util.suffixIfNot


interface SearchPathResolver {
    val searchRoots: List<File>
    fun resolve(givenPath: String): File
    fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<File>
}

fun defaultResolver(repositories: List<String>, target: KonanTarget): SearchPathResolver =
        defaultResolver(repositories, target, Distribution())

fun defaultResolver(repositories: List<String>, target: KonanTarget, distribution: Distribution): SearchPathResolver =
        KonanLibrarySearchPathResolver(
                repositories,
                target,
                distribution.klib,
                distribution.localKonanDir.absolutePath
        )

class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        val target: KonanTarget?,
        val distributionKlib: String?,
        val localKonanDir: String?,
        val skipCurrentDir: Boolean = false
): SearchPathResolver {

    val localHead: File?
        get() = localKonanDir?.File()?.klib

    val distHead: File?
        get() = distributionKlib?.File()?.child("common")

    val distPlatformHead: File?
        get() = target?.let { distributionKlib?.File()?.child("platform")?.child(target.visibleName) }

    val currentDirHead: File?
        get() = if (!skipCurrentDir) File.userDir else null

    private val repoRoots: List<File> by lazy {
        repositories.map{File(it)}
    }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        (listOf(currentDirHead) + repoRoots + listOf(localHead, distHead, distPlatformHead)).filterNotNull()
    }

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean =
                file.exists && (file.isFile || File(file, "manifest").exists)

        val noSuffix = File(candidate.path.removeSuffixIfPresent(".klib"))
        val withSuffix = File(candidate.path.suffixIfNot(".klib"))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }

    override fun resolve(givenPath: String): File {
        val given = File(givenPath)
        if (given.isAbsolute) {
            found(given)?.apply{ return this }
        } else {
            searchRoots.forEach{
                found(File(it, givenPath))?.apply{return this}
            }
        }
        error("Could not find \"$givenPath\" in ${searchRoots.map{it.absolutePath}}.")
    }

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOf(distHead, distPlatformHead)
                .filterNotNull()
                .filter{ it.exists }

    override fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<File> {
        val defaultLibs = defaultRoots.flatMap{ it.listFiles }
                .filterNot { it.name.removeSuffixIfPresent(".klib") == "stdlib" }
                .map { File(it.absolutePath) }
        val result = mutableListOf<File>()
        if (!nostdlib) result.add(resolve("stdlib"))
        if (!noDefaultLibs) result.addAll(defaultLibs)
        return result
    }
}
