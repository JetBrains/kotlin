/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_JVM_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.tasks.abi.AbiToolsTask.Companion.composeTaskName

@DisableCachingByDefault(because = "File copy should not be cacheable")
internal abstract class KotlinLegacyAbiUpdateTask : DefaultTask() {
    @get:OutputDirectory
    abstract val referenceDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val actualDir: DirectoryProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    val projectName: String = project.name

    @get:Internal
    val rootDir: String = project.rootDir.absolutePath

    @TaskAction
    internal fun overwrite() {
        val actualDir = actualDir.get().asFile
        val referenceDir = referenceDir.get().asFile

        if (!referenceDir.absolutePath.startsWith(rootDir)) {
            throw IllegalStateException("'referenceDir' must be a subdirectory of the build root directory; 'referenceDir' $referenceDir, root directory $rootDir")
        }

        val jvmDumpName = projectName + LEGACY_JVM_DUMP_EXTENSION
        val klibDumpName = projectName + LEGACY_KLIB_DUMP_EXTENSION

        actualDir.walk()
            .filter { file -> file.isFile && file.name == jvmDumpName || file.name == klibDumpName }
            .forEach { actualDump ->
                val relative = actualDump.toRelativeString(actualDir)
                val referenceDump = referenceDir.resolve(relative)

                actualDump.copyTo(referenceDump, overwrite = true)
            }

    }

    companion object {
        fun nameForVariant(variantName: String): String {
            return composeTaskName("updateLegacyAbi", variantName)
        }
    }
}
