package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.konan.konanHome
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.*
import java.io.File

enum class KonanCacheKind(val outputKind: CompilerOutputKind) {
    STATIC(CompilerOutputKind.STATIC_CACHE),
    DYNAMIC(CompilerOutputKind.DYNAMIC_CACHE)
}

open class KonanCacheTask: DefaultTask() {
    @InputDirectory
    lateinit var originalKlib: File

    // Taken into account by the [cacheFile] property.
    @Internal
    lateinit var cacheRoot: File

    @get:Input
    lateinit var target: String

    @get:Internal
    // TODO: Reuse NativeCacheKind from Big Kotlin plugin when it is available.
    val cacheDirectory: File
        get() = cacheRoot.resolve("$target-g$cacheKind")

    @get:OutputDirectory
    protected val cacheFile: File
        get() {
            val klibName = originalKlib.let {
                if (it.isDirectory) it.name else it.nameWithoutExtension
            }
            return cacheDirectory.resolve("${klibName}-cache")
        }

    @Input
    var cacheKind: KonanCacheKind = KonanCacheKind.STATIC

    @Input
    /** Path to a compiler distribution that is used to build this cache. */
    val compilerDistributionPath: Property<File> = project.objects.property(File::class.java).apply {
        set(project.provider { project.kotlinNativeDist })
    }

    @TaskAction
    fun compile() {
        // Compiler doesn't create a cache if the cacheFile already exists. So we need to remove it manually.
        if (cacheFile.exists()) {
            val deleted = cacheFile.deleteRecursively()
            check(deleted) { "Cannot delete stale cache: ${cacheFile.absolutePath}" }
        }
        val konanHome = compilerDistributionPath.get().absolutePath
        val additionalCacheFlags = PlatformManager(konanHome).let {
            it.targetByName(target).let(it::loader).additionalCacheFlags
        }
        val args = listOf(
            "-g",
            "-target", target,
            "-produce", cacheKind.outputKind.name.toLowerCase(),
            "-Xadd-cache=${originalKlib.absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}"
        ) + additionalCacheFlags
        KonanCompilerRunner(project, konanHome = konanHome).run(args)
    }
}