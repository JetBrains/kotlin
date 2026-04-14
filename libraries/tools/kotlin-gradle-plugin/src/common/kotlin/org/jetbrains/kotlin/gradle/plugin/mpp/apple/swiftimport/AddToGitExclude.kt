package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.enterprise.test.FileProperty
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

@DisableCachingByDefault(because = "KT-84827 - SwiftPM import doesn't support caching yet")
abstract class AddPathsToGitInfoExclude : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val paths: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    val excludeFile: RegularFileProperty = project.objects.fileProperty()
        .convention(rootDirectory.file(".git/info/exclude"))


    @TaskAction
    fun run() {
        val root = rootDirectory.get().asFile
        val exclude = excludeFile.get().asFile
        exclude.parentFile.mkdirs()
        exclude.createNewFile()

        val existing = exclude.readLines()
            .map { it.trim() }
            .filterNot { it.isEmpty() || it.startsWith("#") }
            .toSet()

        val toAdd = paths.files
            .map { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                if (file.isDirectory) "$relative/" else relative
            }
            .distinct()
            .filterNot { it in existing }

        if (toAdd.isNotEmpty()) {
            exclude.appendText(
                buildString {
                    if (exclude.length() > 0L) appendLine()
                    toAdd.forEach { appendLine(it) }
                }
            )
        }
    }

    companion object {
        const val TASK_NAME = "addPathsToGitInfoExclude"

        fun addPathsToGitInfoExcludeTaskName(desc: String) =
            lowerCamelCaseName(TASK_NAME, desc)
    }
}
