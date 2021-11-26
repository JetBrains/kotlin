/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.konan.KonanKlibRunner
import java.io.File

open class KonanKlibInstallTask : DefaultTask() {
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
    fun exec() {
        val args = listOf(
                "install", klib.get().absolutePath,
                "-target", target,
                "-repository", repo.absolutePath
        )
        KonanKlibRunner(project, konanHome = project.kotlinNativeDist.absolutePath).run(args)
    }
}