/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.gradle.plugin.abi.internal.AbiValidationPaths.LEGACY_JVM_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.abi.internal.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION

@DisableCachingByDefault(because = "No output")
internal abstract class KotlinAbiCheckTaskImpl : AbiToolsTask() {
    @get:Internal
    abstract val referenceDir: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val referenceDumps: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val actualDir: DirectoryProperty

    @get:Input
    val projectName: String = project.name

    private val projectPath = project.path

    private val rootDir = project.rootDir


    override fun runTools(tools: AbiTools) {
        val referenceDir = referenceDir.get().asFile
        val actualDir = actualDir.get().asFile
        val pathPrefix = if (projectPath == ":") ":" else "$projectPath:"

        val jvmDumpName = projectName + LEGACY_JVM_DUMP_EXTENSION
        val klibDumpName = projectName + LEGACY_KLIB_DUMP_EXTENSION

        val errorBuilder = StringBuilder()

        val actualDumps = actualDir.walk()
            .filter { file -> file.isFile && file.name == jvmDumpName || file.name == klibDumpName }
            .toList()

        val referenceDumps = referenceDumps.files
            .filter { it.exists() && it.isFile }
            .map { it.absolutePath }
            .toMutableSet()

        actualDumps.forEach { actualDump ->
            val relative = actualDump.toRelativeString(actualDir)
            val referenceDump = referenceDir.resolve(relative)
            if (referenceDumps.remove(referenceDump.absolutePath)) {

                val diffSet = mutableSetOf<String>()
                val diff = tools.filesDiff(
                    referenceDump,
                    actualDump
                )
                if (diff != null) diffSet.add(diff)
                if (diffSet.isNotEmpty()) {
                    val diffText = diffSet.joinToString("\n\n")
                    errorBuilder.append("\n<<<ABI has changed>>>\n$diffText\n\n")
                }

            } else {
                errorBuilder.append(
                    "Expected file with ABI declarations '${referenceDump.relativeTo(rootDir)}' does not exist.\n\n"
                )
            }
        }

        if (errorBuilder.isNotEmpty()) {
            errorBuilder.append("You can run '$pathPrefix${KotlinAbiUpdateTask.NAME}' task to create or overwrite reference ABI declarations")

            error("ABI check failed for project $projectName\n\n$errorBuilder")
        }
    }

    companion object {
        const val NAME: String = "checkKotlinAbi"
    }
}
