/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platformLibs

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.exists

fun Project.familyDefFiles(family: Family) = fileTree("src/platform/${family.visibleName}")
        .filter { it.name.endsWith(".def") }

fun Project.registerUpdateDefFileDependenciesForAppleFamiliesTasks(aggregateTask: TaskProvider<*>): Map<Family, TaskProvider<*>> {
    val shouldUpdate = project.getBooleanProperty(updateDefFileDependenciesFlag) ?: false

    val updateDefFilesTaskPerFamily = KonanTarget.predefinedTargets.values.filter { it.family.isAppleFamily }.groupBy { it.family }.mapValues {
        registerUpdateDefFileDependenciesTask(
                family = it.key,
                targets = it.value,
                shouldUpdate = shouldUpdate,
        )
    }

    val allDefFileDiffFiles = updateDefFilesTaskPerFamily.values.map { task ->
        task.map {
            it.defFileDiffsFile.get().asFile
        }
    }

    aggregateTask.configure {
        onlyIf { shouldUpdate }
        inputs.files(allDefFileDiffFiles)
        // Stub output to make this task up-to-date when there are no changes in defFilesChanges files
        outputs.upToDateWhen { true }
        doLast {
            val changes = allDefFileDiffFiles.map { it.get().readText() }.joinToString("\n")
            if (changes.isNotBlank()) {
                error("""
                    |Def files changed, please commit the changes
                    |To update def files run:
                    |
                    |    KONAN_USE_INTERNAL_SERVER=1 ./gradlew :kotlin-native:platformLibs:updateDefFileDependencies -P${updateDefFileDependenciesFlag}
                    |
                    |Changes:
                    |$changes
                    """.trimMargin()
                )
            }
        }
    }

    return updateDefFilesTaskPerFamily
}

private fun Project.registerUpdateDefFileDependenciesTask(
        family: Family,
        targets: List<KonanTarget>,
        shouldUpdate: Boolean,
): TaskProvider<UpdateDefFileDependenciesTask> = tasks.register("${family.visibleName}UpdateDefFileDependencies", UpdateDefFileDependenciesTask::class.java) {
    dependsOn(":kotlin-native:distCompiler")

    onlyIf("-P${updateDefFileDependenciesFlag} is not set") { shouldUpdate }
    defFiles.from(familyDefFiles(family))
    targetNames.set(targets.map { it.name })
    runKonan.set(nativeDistribution.map { it.runKonan.asFile })
    defFileDiffsFile.set(layout.buildDirectory.file("${family.visibleName}ChangedDefFiles"))
}

private const val updateDefFileDependenciesFlag = "kotlin.native.platformLibs.updateDefFileDependencies"

@DisableCachingByDefault
private open class UpdateDefFileDependenciesTask @Inject constructor(
        private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val defFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    val targetNames: ListProperty<String> = project.objects.listProperty(String::class.java)

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val runKonan: Property<File> = project.objects.property(File::class.java)

    /**
     * The [internalToolchain], [developerDir] and [selectedXcode] are implicit inputs to runKonan. If one of these changes,
     * updateDefFileDependencies tasks must rerun.
     */
    @get:Optional
    @get:InputFile
    protected val selectedXcode: File? = Paths.get("/var/db/xcode_select_link").let { xcodeSelectLink ->
        if (xcodeSelectLink.exists()) {
            Files.readSymbolicLink(xcodeSelectLink).parent.resolve("version.plist").toFile()
        } else {
            null
        }
    }

    @get:Input
    protected val internalToolchain = project.providers.environmentVariable("KONAN_USE_INTERNAL_SERVER").orElse("")

    @get:Input
    protected val developerDir = project.providers.environmentVariable("DEVELOPER_DIR").orElse("")

    @get:OutputFile
    val defFileDiffsFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun update() {
        val initialDefFiles = mutableMapOf<File, String>()
        defFiles.forEach { initialDefFiles[it] = it.readText() }
        execOperations.exec {
            commandLine(runKonan.get(), "defFileDependencies", *targetNames.get().flatMap { listOf("-target", it) }.toTypedArray(), *defFiles.map { it.path }.toTypedArray())
        }
        val changedDefFiles = mutableListOf<File>()
        defFiles.forEach {
            val finalDefFile = it.readText()
            if (initialDefFiles[it] != finalDefFile) {
                changedDefFiles.add(it)
            }
        }
        val changes = buildString {
            changedDefFiles.forEach { file ->
                appendLine("$file:")
                appendLine(dumpDefFileDiff(file, initialDefFiles[file]!!.encodeToByteArray().inputStream()))
            }
        }
        defFileDiffsFile.get().asFile.writeText(changes)
    }

    private fun dumpDefFileDiff(
            changedDefFile: File,
            initialDefFile: InputStream
    ): String {
        val diff = ByteArrayOutputStream()
        execOperations.exec {
            commandLine("/usr/bin/diff", "/dev/stdin", changedDefFile.path)
            standardInput = initialDefFile
            standardOutput = diff
            setIgnoreExitValue(true)
        }
        return diff.toString()
    }

}


private fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}