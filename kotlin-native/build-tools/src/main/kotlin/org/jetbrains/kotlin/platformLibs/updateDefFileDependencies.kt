/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platformLibs

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.kotlinNativeDist
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

fun Project.familyDefFiles(family: Family) = fileTree("src/platform/${family.visibleName}")
        .filter { it.name.endsWith(".def") }

fun Project.registerUpdateDefFileDependenciesForAppleFamiliesTasks(): Map<Family, TaskProvider<*>> {
    return mapOf(
            Family.IOS to listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64),
            Family.OSX to listOf(KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64),
            Family.WATCHOS to listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64, KonanTarget.WATCHOS_SIMULATOR_ARM64, KonanTarget.WATCHOS_X64),
            Family.TVOS to listOf(KonanTarget.TVOS_ARM64, KonanTarget.TVOS_SIMULATOR_ARM64, KonanTarget.TVOS_X64),
    ).mapValues {
        registerUpdateDefFileDependenciesTask(
                family = it.key,
                targets = it.value,
        )
    }
}

private const val updateDefFileDependenciesFlag = "kotlin.native.platformLibs.updateDefFileDependencies"

private fun Project.registerUpdateDefFileDependenciesTask(
        family: Family,
        targets: List<KonanTarget>
): TaskProvider<Task> = tasks.register("${family.visibleName}UpdateDefFileDependencies") {
    dependsOn(":kotlin-native:distCompiler")

    val defFiles = familyDefFiles(family).sorted()
    inputs.files(defFiles)
    inputs.file(Files.readSymbolicLink(Paths.get("/var/db/xcode_select_link")).parent.resolve("version.plist").toFile())
    inputs.property("internalToolchain", providers.environmentVariable("KONAN_USE_INTERNAL_SERVER").orElse(""))
    inputs.property("developerDir", providers.environmentVariable("DEVELOPER_DIR").orElse(""))
    // Use inputs for up-to-dateness and stub output to force up-to-dateness checks
    outputs.upToDateWhen { true }
    val shouldUpdate = project.getBooleanProperty(updateDefFileDependenciesFlag) ?: false
    onlyIf("-P${updateDefFileDependenciesFlag} is not set") { shouldUpdate }

    val execOperations = serviceOf<ExecOperations>()
    val logger = logger
    val runKonan = File(kotlinNativeDist.absolutePath).resolve("bin/run_konan")
    doLast {
        updateDefFileDependencies(
                logger = logger,
                execOperations = execOperations,
                runKonan = runKonan,
                targets = targets,
                defFiles = defFiles
        )
    }
}

private fun updateDefFileDependencies(
        logger: Logger,
        execOperations: ExecOperations,
        runKonan: File,
        targets: List<KonanTarget>,
        defFiles: List<File>,
) {
    val initialDefFiles = mutableMapOf<File, String>()
    defFiles.forEach { initialDefFiles[it] = it.readText() }
    execOperations.exec {
        commandLine(runKonan, "defFileDependencies", *targets.flatMap { listOf("-target", it.name) }.toTypedArray(), *defFiles.map { it.path }.toTypedArray())
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
            dumpDefFileDiff(execOperations, file, initialDefFiles[file]!!.encodeToByteArray().inputStream())
        }
        error("""
            Def files changed, please commit the changes
            To update def files run: KONAN_USE_INTERNAL_SERVER=1 ./gradlew :kotlin-native:platformLibs:updateDefFileDependencies -P${updateDefFileDependenciesFlag}
            Changes in $changedDefFiles
            """.trimIndent()
        )
    }
}

private fun dumpDefFileDiff(
        execOperations: ExecOperations,
        changedDefFile: File,
        initialDefFile: InputStream
) {
    execOperations.exec {
        commandLine("/usr/bin/diff", "/dev/stdin", changedDefFile.path)
        standardInput = initialDefFile
        setIgnoreExitValue(true)
    }
}


private fun Project.getBooleanProperty(name: String): Boolean? = this.findProperty(name)?.let {
    val v = it.toString()
    if (v.isBlank()) true
    else v.toBoolean()
}