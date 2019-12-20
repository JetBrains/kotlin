package org.jetbrains.kotlin.gradle.plugin.konan.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.konan.KonanCompilerRunner
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanTargetableTask
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
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
    lateinit var cacheDirectory: File

    @get:Input
    lateinit var target: String

    @get:OutputFile
    protected val cacheFile: File
        get() {
            val konanTarget = HostManager().targetByName(target)
            val klibName = originalKlib.nameWithoutExtension
            val cachePrefix = cacheKind.outputKind.prefix(konanTarget)
            val cacheSuffix = cacheKind.outputKind.suffix(konanTarget)
            val cacheName = "${cachePrefix}${klibName}-cache${cacheSuffix}"
            return cacheDirectory.resolve(cacheName)
        }

    @Input
    var cacheKind: KonanCacheKind = KonanCacheKind.STATIC

    @TaskAction
    fun compile() {
        val args = listOf(
            "-g",
            "-target", target,
            "-produce", cacheKind.outputKind.name.toLowerCase(),
            "-Xadd-cache=${originalKlib.absolutePath}",
            "-Xcache-directory=${cacheDirectory.absolutePath}"
        )
        KonanCompilerRunner(project).run(args)
    }
}