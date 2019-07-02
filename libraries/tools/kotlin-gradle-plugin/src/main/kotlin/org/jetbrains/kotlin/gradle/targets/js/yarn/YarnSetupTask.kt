/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import java.io.File
import javax.xml.ws.Action

open class YarnSetupTask : DefaultTask() {
    private val settings = project.yarn
    private val env by lazy { settings.environment }

    @Suppress("MemberVisibilityCanBePrivate")
    val downloadUrl
        @Input get() = env.downloadUrl

    @Suppress("MemberVisibilityCanBePrivate")
    val destination: File
        @OutputDirectory get() = env.home

    init {
        group = NodeJsRootPlugin.TASKS_GROUP_NAME
        description = "Download and install a local yarn version"

        onlyIf {
            !settings.installationDir.exists()
        }
    }

    @Action
    fun setup() {
        val dir = temporaryDir
        val tar = download(dir)
        extract(tar, destination)
    }

    private fun download(tar: File): File {
        val action = DownloadAction(project)
        action.src(downloadUrl)
        action.dest(tar)
        action.execute()
        return action.outputFiles.singleOrNull() ?: error("Cannot get downloaded file $downloadUrl")
    }

    private fun extract(archive: File, destination: File) {
        val dirInTar = archive.name.removeSuffix(".tar.gz")
        project.copy {
            it.from(project.tarTree(archive))
            it.into(destination)
            it.includeEmptyDirs = false
            it.eachFile { fileCopy ->
                fileCopy.path = fileCopy.path.removePrefix(dirInTar)
            }
        }
    }

    companion object {
        const val NAME: String = "kotlinYarnSetup"
    }
}
