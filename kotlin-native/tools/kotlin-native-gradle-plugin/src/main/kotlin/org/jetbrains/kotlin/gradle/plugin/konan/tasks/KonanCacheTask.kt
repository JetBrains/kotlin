package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCompilerRunner
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
    lateinit var cacheRoot: String

    @get:Input
    lateinit var target: String

    @get:Internal
    // TODO: Reuse NativeCacheKind from Big Kotlin plugin when it is available.
    val cacheDirectory: File
        get() = File("$cacheRoot/$target-g$cacheKind")

    @get:OutputDirectory
    val cacheFile: File
        get() {
            val konanHome = compilerDistributionPath.get().absolutePath
            val resolver = defaultResolver(
                    emptyList(),
                    PlatformManager(konanHome).targetByName(target),
                    Distribution(konanHome)
            )
            val klibName = resolver.resolve(originalKlib!!.absolutePath).uniqueName
            return cacheDirectory.resolve("${klibName}-cache")
        }

    @get:Input
    var cacheKind: KonanCacheKind = KonanCacheKind.STATIC

    @get:Input
    /** Path to a compiler distribution that is used to build this cache. */
    val compilerDistributionPath: Property<File> = project.objects.property(File::class.java).apply {
        set(project.provider { project.kotlinNativeDist })
    }

    @get:Input
    var cachedLibraries: Map<File, File> = emptyMap()

    @TaskAction
    fun compile() {
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
        val args = listOf(
            "-g",
            "-target", target,
            "-produce", cacheKind.outputKind.name.toLowerCase(),
            "-Xadd-cache=${originalKlib?.absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}"
        ) + additionalCacheFlags + cachedLibraries.map { "-Xcached-library=${it.key},${it.value}" }
        KonanCompilerRunner(project, konanHome = konanHome).run(args)
    }
}