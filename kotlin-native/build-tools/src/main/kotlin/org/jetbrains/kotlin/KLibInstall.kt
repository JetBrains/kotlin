package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

// TODO: Implement as a part of the gradle plugin
open class KlibInstall : Exec() {
    @get:InputFile
    var klib: Provider<File> = project.provider { project.buildDir }

    @get:Internal
    var repo: File = project.rootDir

    @get:Input
    val repoPath
        get() = repo.absolutePath

    val installDir: Provider<File>
        @OutputDirectory
        get() = project.provider {
            val klibName = klib.get().nameWithoutExtension
            project.file("${repo.absolutePath}/$klibName")
        }

    @get:Input
    var target: String = HostManager.hostName

    @TaskAction
    override fun exec() {
        val konanHome = project.kotlinNativeDist
        val suffix = if (HostManager.host == KonanTarget.MINGW_X64) ".bat" else ""
        val klibProgram = "$konanHome/bin/klib$suffix"

        commandLine(klibProgram,
                "install", klib.get().absolutePath,
                "-target", target,
                "-repository", repo.absolutePath
        )

        super.exec()
    }
}