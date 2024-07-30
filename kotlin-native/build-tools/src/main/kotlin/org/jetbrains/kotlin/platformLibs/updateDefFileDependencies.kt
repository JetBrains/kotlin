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
import org.jetbrains.kotlin.kotlinNativeDist
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

fun Project.familyDefFiles(family: Family) = fileTree("src/platform/${family.visibleName}")
        .filter { it.name.endsWith(".def") }

fun Project.registerUpdateDefFileDependenciesForAppleFamiliesTasks(aggregateTask: TaskProvider<*>): Map<Family, TaskProvider<*>> {
    val shouldUpdate = project.getBooleanProperty(updateDefFileDependenciesFlag) ?: false
    val changedDefFilesLists = project.files()
    aggregateTask.configure {
        onlyIf { shouldUpdate }
        inputs.files(changedDefFilesLists)
        outputs.upToDateWhen { true }
        doLast {
            val changedDefFiles = changedDefFilesLists.flatMap {
                it.readText().split("\n").filterNot { it.isBlank() }
            }.joinToString(" ")
            if (changedDefFiles.isNotBlank()) {
                error("""
                    Def files changed, please commit the changes
                    To update def files run: KONAN_USE_INTERNAL_SERVER=1 ./gradlew :kotlin-native:platformLibs:updateDefFileDependencies -P${updateDefFileDependenciesFlag}
                    Changes in $changedDefFiles
                    """.trimIndent()
                )
            }
        }
    }

    return mapOf(
            Family.IOS to listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64),
            Family.OSX to listOf(KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64),
            Family.WATCHOS to listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64, KonanTarget.WATCHOS_SIMULATOR_ARM64, KonanTarget.WATCHOS_X64),
            Family.TVOS to listOf(KonanTarget.TVOS_ARM64, KonanTarget.TVOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64),
    ).mapValues {
        val task = registerUpdateDefFileDependenciesTask(
                family = it.key,
                targets = it.value,
                shouldUpdate = shouldUpdate,
        )
        changedDefFilesLists.from(task.flatMap { it.changedDefFilesList })
        return@mapValues task
    }
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
    runKonan.set(File(kotlinNativeDist.absolutePath).resolve("bin/run_konan"))
    changedDefFilesList.set(layout.buildDirectory.file("${family.visibleName}ChangedDefFiles"))
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

    @get:Input
    val runKonan: Property<File> = project.objects.property(File::class.java)

    @get:InputFile
    protected val selectedXcode: File = Files.readSymbolicLink(Paths.get("/var/db/xcode_select_link")).parent.resolve("version.plist").toFile()

    @get:Input
    protected val internalToolchain = project.providers.environmentVariable("KONAN_USE_INTERNAL_SERVER").orElse("")

    @get:Input
    protected val developerDir = project.providers.environmentVariable("DEVELOPER_DIR").orElse("")

    @get:OutputFile
    val changedDefFilesList: RegularFileProperty = project.objects.fileProperty()

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
        if (changedDefFiles.isNotEmpty()) {
            changedDefFiles.forEach { file ->
                logger.error("Def file $file changed:")
                dumpDefFileDiff(file, initialDefFiles[file]!!.encodeToByteArray().inputStream())
            }
        }
        changedDefFilesList.get().asFile.writeText(changedDefFiles.joinToString("\n"))
    }

    private fun dumpDefFileDiff(
            changedDefFile: File,
            initialDefFile: InputStream
    ) {
        execOperations.exec {
            commandLine("/usr/bin/diff", "/dev/stdin", changedDefFile.path)
            standardInput = initialDefFile
            setIgnoreExitValue(true)
        }
    }

}


private fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}