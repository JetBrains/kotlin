package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCliCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.konan.konanHome
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.*
import java.io.File

enum class KonanCacheKind(val outputKind: CompilerOutputKind) {
    STATIC(CompilerOutputKind.STATIC_CACHE),
    DYNAMIC(CompilerOutputKind.DYNAMIC_CACHE)
}

open class KonanCacheTask: DefaultTask() {
    @get:InputDirectory
    var originalKlib: File? = null

    @get:Input
    lateinit var klibUniqName: String

    @get:Input
    lateinit var cacheRoot: String

    @get:Input
    lateinit var target: String

    @get:Internal
    // TODO: Reuse NativeCacheKind from Big Kotlin plugin when it is available.
    val cacheDirectory: File
        get() = File("$cacheRoot/$target-g$cacheKind")

    @get:OutputDirectory
    val cacheFile: File
        get() = cacheDirectory.resolve(if (makePerFileCache) "${klibUniqName}-per-file-cache" else "${klibUniqName}-cache")

    /**
     * Note: we can't use this function instead of [klibUniqName] in [cacheFile],
     * because the latter is `@OutputDirectory`, so Gradle can call it even before
     * the task dependencies are finished, and [originalKlib] might be not build yet.
     */
    private fun readKlibUniqNameFromManifest(): String {
        val konanHome = compilerDistributionPath.get().absolutePath
        val resolver = defaultResolver(
                emptyList(),
                PlatformManager(konanHome).targetByName(target),
                Distribution(konanHome)
        )
        return resolver.resolve(originalKlib!!.absolutePath).uniqueName
    }

    @get:Input
    var cacheKind: KonanCacheKind = KonanCacheKind.STATIC

    @get:Input
    var makePerFileCache: Boolean = false

    @get:Input
    /** Path to a compiler distribution that is used to build this cache. */
    val compilerDistributionPath: Property<File> = project.objects.property(File::class.java).apply {
        set(project.provider { project.kotlinNativeDist })
    }

    @get:Input
    var cachedLibraries: Map<File, File> = emptyMap()

    @TaskAction
    fun compile() {
        // This code uses bootstrap version of util-klib and fails due to the older default ABI than library being used
        // A possible solution is to read it manually from manifest file or this check should be done by the compiler itself
//        check(klibUniqName == readKlibUniqNameFromManifest()) {
//            "klibUniqName mismatch: configured '$klibUniqName', resolved '${readKlibUniqNameFromManifest()}'"
//        }

        // Compiler doesn't create a cache if the cacheFile already exists. So we need to remove it manually.
        if (cacheFile.exists()) {
            val deleted = cacheFile.deleteRecursively()
            check(deleted) { "Cannot delete stale cache: ${cacheFile.absolutePath}" }
        }
        cacheDirectory.mkdirs()
        val konanHome = compilerDistributionPath.get().absolutePath
        val additionalCacheFlags = PlatformManager(konanHome).let {
            it.targetByName(target).let(it::loader).additionalCacheFlags
        }
        requireNotNull(originalKlib)
        val args = mutableListOf(
            "-g",
            "-target", target,
            "-produce", cacheKind.outputKind.name.toLowerCase(),
            "-Xadd-cache=${originalKlib?.absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}"
        )
        if (makePerFileCache)
            args += "-Xmake-per-file-cache"
        args += additionalCacheFlags
        args += cachedLibraries.map { "-Xcached-library=${it.key},${it.value}" }
        KonanCliCompilerRunner(project, konanHome = konanHome).run(args)
    }
}