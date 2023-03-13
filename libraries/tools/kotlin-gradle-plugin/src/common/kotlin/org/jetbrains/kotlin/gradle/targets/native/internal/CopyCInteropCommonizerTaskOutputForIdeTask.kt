/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import java.io.File

internal open class CopyCommonizeCInteropForIdeTask : AbstractCInteropCommonizerTask() {

    private val commonizeCInteropTask: TaskProvider<CInteropCommonizerTask>
        get() = project.commonizeCInteropTask ?: throw IllegalStateException("Missing commonizeCInteropTask")

    @get:IgnoreEmptyDirectories
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val cInteropCommonizerTaskOutputDirectories: Provider<Set<File>> =
        commonizeCInteropTask.map { it.allOutputDirectories }

    @get:OutputDirectory
    override val outputDirectory: File = project.rootDir.resolve(".gradle/kotlin/commonizer")
        .resolve(project.path.removePrefix(":").replace(":", "/"))

    override suspend fun findInteropsGroup(dependent: CInteropCommonizerDependent): CInteropCommonizerGroup? {
        return commonizeCInteropTask.get().findInteropsGroup(dependent)
    }

    @TaskAction
    protected fun copy() {
        outputDirectory.mkdirs()
        for (group in commonizeCInteropTask.get().allInteropGroups.getOrThrow()) {
            val source = commonizeCInteropTask.get().outputDirectory(group)
            if (!source.exists()) continue
            val target = outputDirectory(group)
            if (target.exists()) target.deleteRecursively()
            source.copyRecursively(target, true)
        }
    }
}
